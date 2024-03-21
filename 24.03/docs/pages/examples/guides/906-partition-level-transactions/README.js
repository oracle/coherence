<doc-view>

<h2 id="_partition_level_transactions">Partition Level Transactions</h2>
<div class="section">
<p>This guide covers how to update multiple cache entries using partition level transactions.</p>


<h3 id="_what_is_a_partition_level_transaction">What is a Partition Level Transaction</h3>
<div class="section">
<p>In a Coherence distributed cache, data is managed in partitions. A cache entry is assigned to a partition and partition
ownership is distributed over the storage enabled members of a cluster. When updating a cache entry using an
<code>EntryProcessor</code>, Coherence performs this update atomically in what is known as a partition level transaction.
In the <code>EntryProcessor</code> code it is possible to "enlist" other entries owned by the same partition into the transaction
so that the mutations to those entries are committed to the partition&#8217;s storage as a single atomic update.
The other entries enlisted into the transaction can be from the same cache, or from other caches that are managed by the
same cache service.</p>

<p>This is a very useful feature for a number of use cases that need to ensure atomic updates of related entries.</p>


<h4 id="_a_simple_use_case">A Simple Use Case</h4>
<div class="section">
<p>Imagine we were building a banking system where we had a <code>Customer</code> cache and an <code>Accounts</code> cache.
A customer would have one entry in the <code>Customers</code> cache, but may have multiple accounts in the <code>Accounts</code> cache.
In our application we want to write some functionality to transfer money between accounts, i.e. take from one account
and add to another. We would like to do this atomically so that we do not take more money from an account than the
current balance and also ensure the customer&#8217;s account balances are consistent.</p>

<p>Say for customer "Foo", we want to transfer $100 from account "A" to account "B" Without a partition level
transaction, the flow might look like this:</p>

<markup
lang="java"

>accounts.invoke(new AccountKey("Foo", "A"), new DebitProcessor(100));
accounts.invoke(new AccountKey("Foo", "B"), new CreditProcessor(100));</markup>

<p>The problem here is that the two account entries are in a distributed cache and may be on different JVMs in a cluster,
probably on different physical servers. There will be a time window between the first invoke and the second where the
total balance of the customer&#8217;s accounts is $100 less than it really is. There is also an issue where the second invoke
fails for some reason, and we then need to handle this and put the $100 back into account "A" or keep re-trying.</p>

<p>A better solution would be a single invoke, where we execute a single <code>EntryProcessor</code> that updates both accounts
in a single transaction. The total balance of the accounts will remain consistent, and if the invoke call fails
Coherence will automatically roll back all the affected entries.</p>

<markup
lang="java"

>customers.invoke(new CustomerKey("Foo"), new TransferProcessor("A", "B", 100));</markup>

<p>The rest of this guide explain how we accomplish this and some other techniques for handling related data.</p>

</div>
</div>

<h3 id="_key_affinity">Key Affinity</h3>
<div class="section">
<p>An important point about partition level transactions is that all the entries enlisted in the transaction must be
owned by the same partition. In normal operation a cache entry is assigned to a partition based on a hash of the
serialized key of the entry. This helps ensure an even distribution of entries over the partitions.
When using partition level transactions we usually need to influence which partition a key is assigned to so that we
ensure related entries are stored in the same partition, this is known as "key affinity", as it is the cache keys
that control the owning partition. In most use cases we do not care exactly which partition a set of related keys
belong to, rather we just need to ensure they all belong to the same partition.</p>

<p>The <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/working-partitions.html">Working with Partitions</a>
section of the official documentation covers the different techniques that can be used to control the partition
an entry is assigned to.</p>

</div>

<h3 id="_avoiding_deadlocks_in_entryprocessors">Avoiding Deadlocks in EntryProcessors</h3>
<div class="section">
<p>It is very important when enlisting other entries in an <code>EntryProcessor</code> that this is done in a way that
avoids deadlocks. There are two ways to avoid deadlocks covered below.</p>


<h4 id="_invoke_against_a_common_key">Invoke Against a Common Key</h4>
<div class="section">
<p>Imagine a case where a customer "Foo" has two accounts, "A" and "B" and we initiate two transfers, one from "A" to "B"
and the other from "B" to "A" using two separate invocations of <code>TransferProcessor</code>.</p>

<p>Say we invoked <code>TransferProcessor</code> against the source account, we could see the following:</p>

<ol style="margin-left: 15px;">
<li>
Invoke <code>TransferProcessor</code> against account "A", the entry for account "A" is now locked in the <code>accounts</code> cache.

</li>
<li>
Invoke <code>TransferProcessor</code> against account "B", the entry for account "B" is now locked in the <code>accounts</code> cache.

</li>
<li>
The <code>TransferProcessor</code> that has locked account "A" now tries to enlist account "B", but account "B" is locked,
so the <code>TransferProcessor</code> will now block waiting for account "B" to be unlocked.

</li>
<li>
The <code>TransferProcessor</code> that has locked account "B" now tries to enlist account "A", but account "A" is locked,
so the <code>TransferProcessor</code> will now block waiting for account "A" to be unlocked.

</li>
<li>
We now have a deadlock, both <code>TransferProcessor</code> invocations are blocked waiting for each other to complete.

</li>
</ol>
<p>When we invoke the <code>TransferProcessor</code> against the entry fo "Foo" in the <code>customers</code> cache the operation changes
to look like this:</p>

<ol style="margin-left: 15px;">
<li>
Invoke <code>TransferProcessor</code> for "A" &#8594; "B" against customer "Foo", the entry for customer "Foo" is now locked in the <code>customers</code> cache.

</li>
<li>
Invoke <code>TransferProcessor</code> for "B" &#8594; "A" against customer "Foo", the entry for customer "Foo" is already locked so this processor will wait.

</li>
<li>
The <code>TransferProcessor</code> for "A" &#8594; "B" enlists accounts "A" and "B" and completes the transfer, unlocking customer "Foo"

</li>
<li>
The <code>TransferProcessor</code> for "B" &#8594; "A" is now unblocked and can execute.

</li>
</ol>
<p>By invoking the <code>TransferProcessor</code> against the customer entry we avoid the risk of deadlocks occurring.</p>

</div>

<h4 id="_sort_the_keys_to_be_enlisted">Sort the Keys to be Enlisted</h4>
<div class="section">
<p>A second method to avoid deadlocks where multiple entries will be enlisted is to sort the keys that will be enlisted.
Sometimes it is not possible, or it is inefficient, to invoke every <code>EntryProcessor</code> against a common key.
In the example above, if we invoked every cache operation the customer entry, this could impact performance for some use
cases because all the operations for a customer would queue and execute one at a time.
Sometimes, for performance we&#8217;d like to execute operations in parallel as much as possible.</p>

<p>An example of this might be where we have a cache that stores data that forms a nested tree type hierarchy,
i.e a parent node that has child nodes which also have sub-child nodes.
Say we have a use case where if we update a sub-child node, we have to apply a corresponding update to its parents.
We could do what we did above and only ever run the <code>EntryProcessor</code> against the top level parent, then enlist entries
going down the hierarchy to the sub-child we want to update. If our application applies a lot of updates to sub-child
nodes, this would be a big performance bottleneck, as updates could only be applied sequentially.
Instead, in this use case the <code>EntryProcessor</code> would execute against the sub-child, then search for and enlist its
parents. How we might search for parents will be covered in an example below.</p>

<p>For example, we have a parent node "A", that has a child node "A-1", which has two sub-child nodes "A-1.1" and "A-1.2".
Without sorting the enlisted entries this is what could happen.</p>

<ol style="margin-left: 15px;">
<li>
Invoke the <code>EntryProcessor</code> "One" against node "A-1.1", so entry "A-1.1" is locked.

</li>
<li>
Invoke the <code>EntryProcessor</code> "Two" against node "A-1.2", so entry "A-1.2" is locked.

</li>
<li>
<code>EntryProcessor</code> "One" searches for its parent nodes, the resulting keys come back as `["A", "A-1"],
so the first of those "A" is serialized and used to enlist entry "A", and locked.

</li>
<li>
Before <code>EntryProcessor</code> "One" can lock the second result, "A-1", <code>EntryProcessor</code> "Two" has searched for its parents
and has a result of `["A-1", "A"] (the opposite order to "One"), so it enlists and locks "A-1".

</li>
<li>
<code>EntryProcessor</code> "One" now tries to enlist "A-1", but is blocked because this is locked by "Two".

</li>
<li>
<code>EntryProcessor</code> "Two" now tries to enlist "A", but is blocked because this is locked by "One".

</li>
<li>
We now have a deadlock

</li>
</ol>
<p>The solution to the deadlock above is for the <code>EntryProcessor</code> to sort the keys of the entries that it will enlist
before enlisting them. The simplest way to do this is to sort the serialized <code>Binary</code> keys that will be used to enlist
the entries, because <code>com.tangosol.util.Binary</code> implements <code>Comparable</code>. The flow then becomes:</p>

<ol style="margin-left: 15px;">
<li>
Invoke the <code>EntryProcessor</code> "One" against node "A-1.1", so entry "A-1.1" is locked.

</li>
<li>
Invoke the <code>EntryProcessor</code> "Two" against node "A-1.2", so entry "A-1.2" is locked.

</li>
<li>
<code>EntryProcessor</code> "One" searches for its parent nodes, the result comes back as `["A", "A-1"]

</li>
<li>
<code>EntryProcessor</code> "One" serializes the keys to <code>Binary</code> keys sorts them, e.g. in a sorted list or <code>TreeSet</code>.

</li>
<li>
<code>EntryProcessor</code> "One" now enlists the first sorted key, say it is "A".

</li>
<li>
Before <code>EntryProcessor</code> "One" can lock the second result, "A-1", <code>EntryProcessor</code> "Two" has searched for its parents
and has a result of `["A-1", "A"] (the opposite order to "One")

</li>
<li>
<code>EntryProcessor</code> "Two" serializes the keys to <code>Binary</code> keys sorts them.

</li>
<li>
<code>EntryProcessor</code> "Two" now enlists the first sorted key, which will also now be "A", but "A" is already locked, so
<code>EntryProcessor</code> "Two" is blocked.

</li>
<li>
<code>EntryProcessor</code> "One" can now proceed to enlist and lock "A-1", perform its updates of "A" and "A-1", then complete,
unlocking all the entries

</li>
<li>
<code>EntryProcessor</code> "Two" can now enist and lock "A-1" and complete its processing.

</li>
</ol>
</div>
</div>

<h3 id="_bank_account_example">Bank Account Example</h3>
<div class="section">
<p>For the first example in this guide, we will implement the simple bank account use case described above to transfer
money between two accounts for the same customer.</p>


<h4 id="_entity_classes">Entity Classes</h4>
<div class="section">
<p>We will need a few entity classes, a simple <code>Customer</code> class and an <code>Account</code> class.
We will also create key classes for both of these. We could use a simple class from the JVM for the customer key such
as a <code>String</code>, a number or a UUID, but in this case we&#8217;ll create a custom class.</p>

<p>We need to use key affinity to ensure that the accounts for a customer a located in the same partition as the customer.
To do this we will make the <code>AccountId</code> class implement the Coherence <code>com.tangosol.net.cache.KeyAssociation</code> interface.
The <code>getAssociatedKey</code> method just returns the <code>customerId</code> field.</p>

<markup
lang="java"

>@Override
public CustomerId getAssociatedKey()
    {
    return customerId;
    }</markup>

<p>The full <code>AccountId</code> class is shown below:</p>

<markup
lang="java"
title="src/main/java/com/oracle/coherence/guides/partitions/bank/AccountId.java"
>public class AccountId
        implements ExternalizableLite, PortableObject, KeyAssociation&lt;CustomerId&gt;
    {
    // ----- constructors ---------------------------------------------------

    /**
     * A default no-args constructor required for serialization.
     */
    public AccountId()
        {
        }

    /**
     * Create a {@link AccountId}.
     *
     * @param id  the id of the customer
     */
    public AccountId(CustomerId customerId, String id)
        {
        this.customerId = customerId;
        this.id = id;
        }

    // ----- KeyAssociation methods -----------------------------------------

    @Override
    public CustomerId getAssociatedKey()
        {
        return customerId;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the id.
     *
     * @return  the customer id
     */
    public CustomerId getCustomerId()
        {
        return customerId;
        }

    /**
     * Return the account id.
     *
     * @return  the customer id
     */
    public String getId()
        {
        return id;
        }

    // ----- Object methods -------------------------------------------------

    // Coherence key classes must properly implement equals() using
    // all the fields in the class
    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        AccountId accountId = (AccountId) o;
        return Objects.equals(customerId, accountId.customerId) &amp;&amp; Objects.equals(id, accountId.id);
        }

    // Coherence key classes must properly implement hashCode() using
    // all the fields in the class
    @Override
    public int hashCode()
        {
        return Objects.hash(customerId, id);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        customerId = ExternalizableHelper.readObject(in);
        id = ExternalizableHelper.readSafeUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, customerId);
        ExternalizableHelper.writeSafeUTF(out, id);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        customerId = in.readObject(0);
        id = in.readString(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, customerId);
        out.writeString(1, id);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The id of the customer.
     */
    private CustomerId customerId;

    /**
     * The id of the account.
     */
    private String id;
    }</markup>

</div>

<h4 id="_the_transfer_entryprocessor">The Transfer EntryProcessor</h4>
<div class="section">
<p>Now we know customers and accounts are co-located we can write the <code>TransferProcessor</code>.</p>

<markup
lang="java"
title="src/main/java/com/oracle/coherence/guides/partitions/bank/TransferProcessor.java"
>public class TransferProcessor
        extends AbstractEvolvable
        implements InvocableMap.EntryProcessor&lt;CustomerId, Customer, Void&gt;,
                   ExternalizableLite, PortableObject
    {
    }</markup>

<p>The generic parameters for the <code>TransferProcessor</code> are <code>&lt;CustomerId, Customer, Void&gt;</code> because the processor will be
invoked against the customers cache, which has a key of <code>CustomerId</code> and a value of <code>Customer</code>.
In this case we do not return a result from the processor, so its return type is <code>Void</code>.</p>

<p>The <code>process</code> method would look like this (with comments to explain the code):</p>

<markup
lang="java"

>    @Override
    @SuppressWarnings("unchecked")
    public Void process(InvocableMap.Entry&lt;CustomerId, Customer&gt; entry)
        {
        // Convert the entry to a BinaryEntry
        BinaryEntry&lt;CustomerId, Customer&gt; binaryEntry = entry.asBinaryEntry();
        // Obtain the backing map manager context
        BackingMapManagerContext context = binaryEntry.getContext();

        // Obtain the converter to use to convert the account identifiers
        // into Coherence internal serialized binary format
        // It is important to use the correct key converter for this conversion
        Converter&lt;AccountId, Binary&gt; keyConverter = context.getKeyToInternalConverter();

        // Obtain the backing map context for the accounts cache
        BackingMapContext accountsContext = context.getBackingMapContext("accounts");

        // Convert the source account id to a binary key and obtain the cache entry for the source account
        Binary sourceKey = keyConverter.convert(sourceAccount);
        InvocableMap.Entry&lt;AccountId, Account&gt; sourceEntry = accountsContext.getBackingMapEntry(sourceKey);
        // Convert the destination account id to a binary key and obtain the cache entry for the destination account
        Binary destinationKey = keyConverter.convert(destinationAccount);
        InvocableMap.Entry&lt;AccountId, Account&gt; destinationEntry = accountsContext.getBackingMapEntry(destinationKey);

        // adjust the values for the two accounts
        Account sourceAccount = sourceEntry.getValue();
        sourceAccount.adjustBalance(amount.negate());
        // set the updated source account back into the entry so that the cache is updated
        sourceEntry.setValue(sourceAccount);

        Account destinationAccount = destinationEntry.getValue();
        destinationAccount.adjustBalance(amount);
        // set the updated destination account back into the entry so that the cache is updated
        destinationEntry.setValue(destinationAccount);

        return null;
        }</markup>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>You must remember to call <code>setValue()</code> on the entries that have been updated passing in the updated values so that
Coherence knows to update the underlying cache entry. Just mutating the value returned from <code>entry.getValue()</code>
will not cause a cache update.</p>
</p>
</div>
<p>In the call to <code>BackingMapContext accountsContext = context.getBackingMapContext("accounts");</code> better coding practice
would be to have a common set of a static constants for the cache names in our application code instead of using
hard coded string values.</p>

</div>
</div>

<h3 id="_hierarchical_data_example">Hierarchical Data Example</h3>
<div class="section">
<p>Using key affinity for hierarchical data can work, but may not always be advisable.
If you have a lot of parent nodes and the hierarchies are small, i.e. a parent only has a small number of children,
and they have a small number of children, that would be workable in Coherence.
You would have a lot of small trees distributed over the cluster os storage enabled members.
If there was only a small number of parents, then there would only be a small number of trees, and hence those would
all live on only a small number of members of the cluster, the remaining members would hold no data.
This would mean some JVMs would be using a lot more heap to hold data than others.
If some trees had a lot more nodes that others, this would also mean the JVMs holding the larger trees would be using
more heap than others in the cluster.</p>

<p>So, the rule for storing hierarchical data with key affinity is lots of small trees is better.</p>


<h4 id="_entity_classes_2">Entity Classes</h4>
<div class="section">
<p>In this example the data model is a bookseller, with a lot of books.
We have a cache holding sales for books by region, where the region forms a hierarchy, e.g. "World", "Continent",
and "Country". We might have data like this:</p>

<markup


>- "The Great Gatsby", "World", 26
  - "The Great Gatsby", "North America", 22
    - "The Great Gatsby", "US", 19
    - "The Great Gatsby", "Canada", 3
  - "The Great Gatsby", "Europe", 4
    - "The Great Gatsby", "France", 1
    - "The Great Gatsby", "UK", 3</markup>

<p>The key class might look like this:</p>

<markup
lang="java"
title="src/main/java/com/oracle/coherence/guides/partitions/books/SalesId.java"
>public class SalesId
        implements ExternalizableLite, PortableObject, KeyAssociation&lt;String&gt;
    {
    // ----- constructors ---------------------------------------------------

    /**
     * A default no-args constructor required for serialization.
     */
    public SalesId()
        {
        }

    /**
     * Create a sales identifier.
     *
     * @param bookId            the book identifier
     * @param regionCode        the region identifier
     * @param parentRegionCode  the parent region identifier, or {@code null}
     *                          if this is a top level region
     */
    public SalesId(String bookId, String regionCode, String parentRegionCode)
        {
        this.bookId = bookId;
        this.regionCode = regionCode;
        this.parentRegionCode = parentRegionCode;
        }

    // ----- KeyAssociation methods -----------------------------------------

    @Override
    public String getAssociatedKey()
        {
        return bookId;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the book identifier.
     *
     * @return the book identifier
     */
    public String getBookId()
        {
        return bookId;
        }

    /**
     * Return the region identifier.
     *
     * @return the region identifier
     */
    public String getRegionCode()
        {
        return regionCode;
        }

    /**
     * Return the parent region identifier, or {@code null}
     * if this is a top level region.
     *
     * @return the parent region identifier, or {@code null}
     *         if this is a top level region
     */
    public String getParentRegionCode()
        {
        return parentRegionCode;
        }

    // ----- Object methods -------------------------------------------------

    // Coherence key classes must properly implement hashCode() using
    // all the fields in the class
    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        SalesId salesId = (SalesId) o;
        return Objects.equals(bookId, salesId.bookId)
                &amp;&amp; Objects.equals(regionCode, salesId.regionCode)
                &amp;&amp; Objects.equals(parentRegionCode, salesId.parentRegionCode);
        }

    // Coherence key classes must properly implement hashCode() using
    // all the fields in the class
    @Override
    public int hashCode()
        {
        return Objects.hash(bookId, regionCode, parentRegionCode);
        }

    // serialization methods omitted...

    // ----- data members ---------------------------------------------------

    /**
     * The identifier for the ook
     */
    private String bookId;

    /**
     * The region code for the sales data.
     */
    private String regionCode;

    /**
     * The parent region code, or {@code null} if this is a top level region.
     */
    private String parentRegionCode;
    }</markup>

<p>The <code>SalesId</code> class uses the <code>bookId</code> as the associated key so that all sales for a book are co-located
in a single partition.
The <code>SalesId</code> has a reference to the parent region, for example the parent of the region "France", is "Europe"
and the parent of "Europe" is "World".</p>

<p>We can also create a simple <code>BookSales</code> class to hold the sales information for a book, we might hold sales for e-books,
audiobooks and paper books. We can add methods on the <code>BookSales</code> class to get, set and increment the different sales
numbers.</p>

</div>

<h4 id="_the_increment_sales_entryprocessor">The Increment Sales EntryProcessor</h4>
<div class="section">
<p>Now we have some entity classes to hold the sales data we can create an <code>EntryProcessor</code> that will update
the sales for a book. The operation of the <code>EntryProcessor</code> would be the following.</p>

<ul class="ulist">
<li>
<p>The <code>EntryProcessor</code> has parameters for the additional sales in each category, paper book, e-book and audiobook for a region.</p>

</li>
<li>
<p>The <code>EntryProcessor</code> is invoked against the <code>SalesId</code> to be updated</p>

</li>
<li>
<p>The <code>EntryProcessor</code> updates the sales data for the region</p>

</li>
<li>
<p>The <code>EntryProcessor</code> then finds the parent regions in the hierarchy and updates them</p>

</li>
</ul>
<p>The <code>EntryProcessor</code> class might look like the code below. The <code>process()</code> method is empty and will be
completed in the next section.</p>

<markup
lang="java"
title="src/main/java/com/oracle/coherence/guides/partitions/books/IncrementSalesProcessor.java"
>public class IncrementSalesProcessor
        extends AbstractEvolvable
        implements InvocableMap.EntryProcessor&lt;SalesId, BookSales, Void&gt;,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default no-args constructor required for serialization.
     */
    public IncrementSalesProcessor()
        {
        }

    /**
     * Create a {@link IncrementSalesProcessor}.
     *
     * @param eBook  the e-book sales
     * @param audio  the audiobook sales
     * @param paper  the paper book sales
     */
    public IncrementSalesProcessor(long eBook, long audio, long paper)
        {
        this.eBook = eBook;
        this.audio = audio;
        this.paper = paper;
        }

    // ----- EntryProcessor methods -----------------------------------------

    @Override
    public Void process(InvocableMap.Entry&lt;CustomerId, Customer&gt; entry)
        {
        return null;
        }

    // ----- serialization methods ------------------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        eBook = in.readLong();
        audio = in.readLong();
        paper = in.readLong();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeLong(eBook);
        out.writeLong(audio);
        out.writeLong(paper);
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        eBook = in.readLong(0);
        audio = in.readLong(1);
        paper = in.readLong(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, eBook);
        out.writeLong(1, audio);
        out.writeLong(2, paper);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The evolvable POF implementation version of this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;

    /**
     * The number of e-books sold.
     */
    private long eBook;

    /**
     * The number of audiobooks sold.
     */
    private long audio;

    /**
     * The number of paper sold.
     */
    private long paper;
    }</markup>

<p>In the <code>IncrementSalesProcessor</code> we can start to create the <code>process</code> method to update the sales data in the entry that
the <code>IncrementSalesProcessor</code> will be invoked on, as shown below:</p>

<markup
lang="java"
title="src/main/java/com/oracle/coherence/guides/partitions/books/IncrementSalesProcessor.java"
>@Override
@SuppressWarnings("unchecked")
public Void process(InvocableMap.Entry&lt;SalesId, BookSales&gt; entry)
    {
    // Obtain a BinaryEntry from the entry being processes
    BinaryEntry&lt;SalesId, BookSales&gt; binaryEntry = entry.asBinaryEntry();

    // update the entry sales data
    BookSales sales;
    if (entry.isPresent())
        {
        // the parent entry is present
        sales = entry.getValue();
        }
    else
        {
        // The parent entry is not present, so set a new BookSales value
        sales = new BookSales();
        }

    sales.incrementEBookSales(eBook);
    sales.incrementAudioSales(audio);
    sales.incrementPaperSales(paper);
    // set the updated sale value back into the entry so that Coherence updates the cache
    entry.setValue(sales);

    // Obtain a sorted set of the Binary keys of the parents of the entry being processed

    // We do not need to return anything
    return null;
    }</markup>

</div>

<h4 id="_partition_local_queries">Partition Local Queries</h4>
<div class="section">
<p>One of the functions of the <code>IncrementSalesProcessor</code> class that is missing from the process method above
is to update the parent regions in the hierarchy.
When the entry processor is invoked against an entry, we do not have the full key for the parent entry, we only know the
book and region. This means we cannot just enlist the entry based on a key, as we do not know the key.
The solution to this is to perform a query to locate the parent, as we know the parent will be in the same partition,
as it has the same book id.</p>

<p>The entry that is passed to the <code>process</code> method can be turned into a <code>BinaryEntry</code> and from this we can obtain the
cache&#8217;s backing map. The backing map in Coherence is the actual <code>Map</code> instance that holds the data for the partition.
This is typically serialized binary data. So even if the generics for the entry are real types (in this case the
<code>SalesId</code> and <code>BookSales</code> classes) the backing map keys and values will be <code>Binary</code> instances.</p>

<p>The two lines below show how to convert the entry passed to the process method into a <code>BinaryEntry</code>.</p>

<markup
lang="java"

>BinaryEntry&lt;SalesId, BookSales&gt; binaryEntry = entry.asBinaryEntry();
BackingMapContext backingMapContext = binaryEntry.getBackingMapContext();</markup>

<p>Now we have the <code>BackingMapContext</code> we can write some additional methods to search for the parents of the entry
being processed.</p>

</div>

<h4 id="_querying_a_binary_backing_map">Querying a Binary Backing Map</h4>
<div class="section">
<p>When inside an entry processor or aggregator, the backing map of a cache is of the type <code>Map&lt;Binary, Binary&gt;</code>,
because the backing map holds the serialized cache data.
The simplest way to query the backing map is to use normal Coherence <code>Filter</code> classes, which typically take
a <code>ValueExtractor</code> to extract the field to be tested in the filter.</p>

<p>We could write a <code>ValueExtractor</code> that can deserialize the <code>Binary</code> key or value and extract the required field,
and that would work fine. But, we can be a bit smarter here, so that we can utilize any indexes that may exist
on the cache. If we wrote a custom binary extractor, that in this example extracted the <code>bookId</code> from the key,
and then created in index on that, we would need to create a second index for any normal cache queries that used
the <code>bookId</code>. It would be far better to be able to just create a single index.</p>

<p>In this example we create a special wrapper class named <code>BinaryValueExtractor</code> that will extract values
from a serialized <code>Binary</code> using a wrapped <code>ValueExtractor</code>.
When the <code>BinaryValueExtractor</code> is used in a <code>Filter</code> that may use an index, we want the index lookup to
retrieve any index created by the wrapped extractor, and to do this the <code>BinaryValueExtractor.getCanonicalName()</code> method
will return the same value as the delegate <code>ValueExtractor</code>.</p>

<markup
lang="java"
title="src/main/java/com/oracle/coherence/guides/partitions/books/BinaryValueExtractor.java"
>public class BinaryValueExtractor&lt;T, E&gt;
        implements ValueExtractor&lt;Binary, E&gt;
    {
    /**
     * Create a {@link BinaryValueExtractor}.
     *
     * @param delegate   the extractor to delegate to
     * @param converter  the {@link Converter} to convert the {@link Binary} value
     *                   to a value to pass to the delegate {@link ValueExtractor}
     */
    public BinaryValueExtractor(ValueExtractor&lt;T, E&gt; delegate, Converter&lt;Binary, T&gt; converter)
        {
        m_delegate  = delegate;
        m_converter = converter;
        }

    // ----- ValueExtractor -------------------------------------------------

    @Override
    public E extract(Binary target)
        {
        T value = m_converter.convert(target);
        return m_delegate.extract(value);
        }

    @Override
    public int getTarget()
        {
        return m_delegate.getTarget();
        }

    @Override
    public String getCanonicalName()
        {
        return m_delegate.getCanonicalName();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * A factory method to create a {@link BinaryValueExtractor}.
     *
     * @param delegate   the extractor to delegate to
     * @param converter  the {@link Converter} to convert the {@link Binary} value
     *                   to a value to pass to the delegate {@link ValueExtractor}
     *
     * @return the {@link BinaryValueExtractor} that will extract from a {@link Binary} value
     *
     * @param &lt;T&gt;  the underlying type to extract from after being deserialized
     * @param &lt;E&gt;  the type of the extracted value
     */
    public static &lt;T, E&gt; ValueExtractor&lt;Binary, E&gt; of(ValueExtractor&lt;T, E&gt; delegate, Converter&lt;Binary, T&gt; converter)
        {
        return new BinaryValueExtractor&lt;&gt;(delegate, converter);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The delegate {@link ValueExtractor}.
     */
    private final ValueExtractor&lt;T, E&gt; m_delegate;

    /**
     * The {@link Converter} to convert the {@link Binary} value
     * to a value to pass to the delegate {@link ValueExtractor}
     */
    private final Converter&lt;Binary, T&gt; m_converter;
    }</markup>

<p>We can now use the <code>BinaryValueExtractor</code> in a <code>Filter</code> inside an entry processor or aggregator.
For example the code below creates an <code>EqualsFilter</code> that can execute against the binary backing map.
The filter will match any entry in the backing map that has a key (<code>SalesId</code>) where the <code>getBookId()</code> method returns "Foo":</p>

<markup
lang="java"

>Filter&lt;?&gt; filter = Filters.equal(BinaryValueExtractor.of(SalesId::getBookId, converter).fromKey(), "Foo");</markup>

<p>Outside an entry processor or aggregator, the same query using the normal cache API would be:</p>

<markup
lang="java"

>Filter&lt;?&gt; filter = Filters.equal(ValueExtractor.of(SalesId::getBookId, converter).fromKey(), "Foo");</markup>

<p>If we want to make the filter more efficient in both cases, we would create an index on the cache as normal
using the normal non-binary extractor:</p>

<markup
lang="java"

>NamedCache&lt;SalesId, BookSales&gt; cache = session.getCache("book-sales");
cache.addIndex(ValueExtractor.of(SalesId::getBookId).fromKey());</markup>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>The <code>BinaryValueExtractor</code> is intentionally not serializable as it is not meant ot be used in normal filter queries.</p>
</p>
</div>
</div>

<h4 id="_finding_the_book_sales_parent">Finding the Book Sales parent</h4>
<div class="section">
<p>The method below shows how to use the <code>BinaryValueExtractor</code> in filters to query the cache backing map,
in this case to obtain the single parent entry, but it may be used in other use-cases to query for multiple entries.</p>

<markup
lang="java"
title="src/main/java/com/oracle/coherence/guides/partitions/books/IncrementSalesProcessor.java"
>private Map.Entry&lt;SalesId, BookSales&gt; getParent(SalesId id, BackingMapContext backingMapContext)
    {
    ObservableMap&lt;Binary, Binary&gt; backingMap = backingMapContext.getBackingMap();
    Map&lt;ValueExtractor, MapIndex&gt; indexMap = backingMapContext.getIndexMap();
    String bookId = id.getBookId();
    String region = id.getParentRegionCode();

    Filter&lt;?&gt; filter = Filters.equal(BinaryValueExtractor.of(SalesId::getBookId, converter).fromKey(), bookId)
                           .and(Filters.equal(BinaryValueExtractor.of(SalesId::getRegionCode, converter).fromKey(), region));

    Set&lt;Map.Entry&lt;Binary, Binary&gt;&gt; setEntries = InvocableMapHelper.query(backingMap, indexMap, filter, true, false, null);

    // there should only ever be zero or one matching entry
    return setEntries.stream()
            .findFirst()
            .orElse(null);
    }</markup>

<p>The code above works as follows:
* Obtain the backing map from the backing map context
* Obtain the map of indexes present on the cache
* Obtain the bookId and region for the parent entry we want to find
* Create a Coherence <code>Filter</code> that will query the baking map keys for a matching entry.
* Execute the query on the backing map using Coherence&#8217;s <code>InvocableMapHelper</code> utility class
* There should only be a single matching entry (or maybe no match) so extract the first entry from the query results and return it</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>Accessing the backing map using <code>backingMapContext.getBackingMap()</code> is marked as deprecated.
The main reason for the deprecation is to discourage direct use of the backing map in application code.
Direct manipulation of the data in the backing map by application code is dangerous and could result in corruption of the cache.</p>

<p>In this use-case there is currently no alternative API to perform a partition local query, but as we are only reading
data from the map it is safe. The contents of the map may be changed by other threads that are executing Coherence requests
on the same partition while the query is in progress. This means that any result returned by a query should be considered
transitive, just like any query results from an active cache.</p>
</p>
</div>
<p>Now we have a method that can obtain the parent entry for a <code>SalesId</code> key we can write another utility method that
will obtain all the parent keys in the hierarchy for a given <code>SalesId</code>.
As already mentioned above, we will sort this set of keys so that when the entries are enlisted we avoid deadlocks.</p>

<markup
lang="java"
title="src/main/java/com/oracle/coherence/guides/partitions/books/IncrementSalesProcessor.java"
>private SortedSet&lt;Binary&gt; getParentKeys(SalesId key, BackingMapContext backingMapContext)
    {
    TreeSet&lt;Binary&gt; parents = new TreeSet&lt;&gt;();
    Converter&lt;Binary, SalesId&gt; converter = backingMapContext.getManagerContext().getKeyFromInternalConverter();

    Map.Entry&lt;Binary, Binary&gt; parent = getParent(key, backingMapContext);
    while (parent != null)
        {
        Binary binaryKey = parent.getKey();
        parents.add(binaryKey);
        key = converter.convert(binaryKey);
        parent = getParent(key, backingMapContext);
        }

    return parents;
    }</markup>

<p>With the additional methods above, we can now complete the <code>process</code> method in the <code>IncrementSalesProcessor</code> class
as shown below.</p>

<markup
lang="java"
title="src/main/java/com/oracle/coherence/guides/partitions/books/IncrementSalesProcessor.java"
>public Void process(InvocableMap.Entry&lt;SalesId, BookSales&gt; entry)
    {
    // update the entry sales data
    BookSales sales;
    if (entry.isPresent())
        {
        // the entry is present
        sales = entry.getValue();
        }
    else
        {
        // The parent entry is not present, so create a new BookSales value
        sales = new BookSales();
        }

    sales.incrementEBookSales(eBook);
    sales.incrementAudioSales(audio);
    sales.incrementPaperSales(paper);
    // set the updated sale value back into the entry so that Coherence updates the cache
    entry.setValue(sales);

    // Obtain a BinaryEntry from the entry being processes
    BinaryEntry&lt;SalesId, BookSales&gt; binaryEntry = entry.asBinaryEntry();
    // Obtain the BackingMapContext for the entry
    BackingMapContext backingMapContext = binaryEntry.getBackingMapContext();
    // Obtain a sorted set of the Binary keys of the parents of the entry being processed
    SortedSet&lt;Binary&gt; parentKeys = getParentKeys(entry.getKey(), backingMapContext);

    // Iterate over the parent keys, enlisting and updating each parent entry
    for (Binary binaryKey : parentKeys)
        {
        InvocableMap.Entry&lt;SalesId, BookSales&gt; parentEntry = backingMapContext.getBackingMapEntry(binaryKey);
        if (parentEntry.isPresent())
            {
            // the parent entry is present
            sales = parentEntry.getValue();
            }
        else
            {
            // The parent entry is not present, so create a new BookSales value
            sales = new BookSales();
            }
        // update the parent sales data
        sales.incrementEBookSales(eBook);
        sales.incrementAudioSales(audio);
        sales.incrementPaperSales(paper);
        // set the updated sale value back into the entry so that Coherence updates the cache
        parentEntry.setValue(sales);
        }

    return null;
    }</markup>

</div>

<h4 id="_using_indexes_in_queries">Using Indexes in Queries</h4>
<div class="section">
<p>In the code above that queries the backing map, the actual query call looks like this:</p>

<markup
lang="java"

>Set&lt;Map.Entry&lt;SalesId, BookSales&gt;&gt; setEntries = InvocableMapHelper.query(map, indexMap, filter, true, false, null);</markup>

<p>You can see that the second parameter is a <code>Map</code> of indexes, which is obtained from the <code>BackingMapContext</code> by
calling <code>backingMapContext.getIndexMap()</code>.
This <code>Map</code> is the map of indexes that have been added to the cache by calls to one of the <code>NamedCache.addIndex()</code>
methods. Using the cache indexes in this way can make a big difference to the speed and efficiently of the query execution.</p>

<p>In the example above the <code>Filter</code> for the query is created like this:</p>

<markup
lang="java"

>Filter&lt;?&gt; filter = Filters.equal(ValueExtractor.of(SalesId::getBookId).fromKey(), bookId)
                       .and(Filters.equal(ValueExtractor.of(SalesId::getRegionCode).fromKey(), region));</markup>

<p>The <code>Filter</code> is an "and" filter which uses two <code>ValueExtractor</code> instances:</p>

<markup
lang="java"

>ValueExtractor.of(SalesId::getBookId).fromKey()
ValueExtractor.of(SalesId::getRegionCode).fromKey()</markup>

<p>If we create indexes on the cache using the same extractors, then the query will be much faster:</p>

<markup
lang="java"

>NamedCache&lt;SalesId, BookSales&gt; cache = session.getCache("book-sales");
cache.addIndex(ValueExtractor.of(SalesId::getBookId).fromKey());
cache.addIndex(ValueExtractor.of(SalesId::getRegionCode).fromKey());</markup>

</div>

<h4 id="_using_indexes_directly">Using Indexes Directly</h4>
<div class="section">
<p>The example above uses the indexes to improve query performance, but in this case as we are querying for the keys,
we could have just used the indexes directly in the <code>IncrementSalesProcessor</code> instead of querying the backing map.
If we know that our application has always created the required indexes before invoking the <code>IncrementSalesProcessor</code>
we can simplify the code and not bother executing a query at all.</p>

<p>We could change the <code>getParentKeys</code> method to use indexes directly:</p>

<markup
lang="java"
title="src/main/java/com/oracle/coherence/guides/partitions/books/IncrementSalesProcessor.java"
>private SortedSet&lt;Binary&gt; getParentKeys(SalesId key, BackingMapContext backingMapContext)
    {
    BackingMapManagerContext managerContext = backingMapContext.getManagerContext();
    Converter&lt;Binary, SalesId&gt; converter = managerContext.getKeyFromInternalConverter();
    Map&lt;ValueExtractor, MapIndex&gt; indexMap = backingMapContext.getIndexMap();

    // Get the two indexes from the index Map
    MapIndex indexBookId = indexMap.get(ValueExtractor.of(SalesId::getBookId).fromKey());
    Map&lt;String, Set&lt;Binary&gt;&gt; indexBookIdContents = indexBookId.getIndexContents();
    MapIndex indexRegion = indexMap.get(ValueExtractor.of(SalesId::getRegionCode).fromKey());
    Map&lt;String, Set&lt;Binary&gt;&gt; indexRegionContents = indexRegion.getIndexContents();

    // Obtain the set of Binary keys that have the required BookId
    Set&lt;Binary&gt; setBookId = indexBookIdContents.get(key.getBookId());

    SortedSet&lt;Binary&gt; parents = new TreeSet&lt;&gt;();

    SalesId parent = key;
    while (parent != null)
        {
        String region = parent.getParentRegionCode();
        if (region == null)
            {
            // we're finished, the key has no parent region
            break;
            }
        // Obtain the set of Binary keys that have the parent region
        // and wrap them in a SubSet, so we do not mutate the real set
        Set&lt;Binary&gt; setRegion = new SubSet&lt;&gt;(indexBookIdContents.get(key.getBookId()));
        // remove any values from the set that are not in the BookId key set
        setRegion.retainAll(setBookId);
        // setRegion "should" now contain zero or one entry
        Binary binaryKey = setRegion.stream().findFirst().orElse(null);
        if (binaryKey == null)
            {
            // we're finished, there was no parent
            break;
            }
        // add the parent to the result set
        parents.add(binaryKey);
        // set the next parent
        parent = converter.convert(binaryKey);
        }

    return parents;
    }</markup>

<div class="admonition warning">
<p class="admonition-textlabel">Warning</p>
<p ><p>When directly accessing the backing map or the index map inside an entry processor` or an aggregator in Coherence,
you should never mutate these structures directly from application code. They should be treated as read-only resources.</p>
</p>
</div>
</div>
</div>
</div>
</doc-view>
