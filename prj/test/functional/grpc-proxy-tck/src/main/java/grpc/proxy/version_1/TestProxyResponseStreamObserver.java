/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_1;

import com.oracle.coherence.common.base.Predicate;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import grpc.proxy.TestStreamObserver;
import io.reactivex.rxjava3.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestProxyResponseStreamObserver
        extends TestStreamObserver<ProxyResponse>
    {
    @Override
    public void onNext(@NonNull ProxyResponse proxyResponse)
        {
        super.onNext(proxyResponse);
        for (Listener listener : f_listListener)
            {
            try
                {
                listener.onNext(proxyResponse);
                }
            catch (Exception e)
                {
                onError(e);
                }
            }
        }

    public CompletableFuture<ProxyResponse> addListener(Predicate<ProxyResponse> predicate)
        {
        Listener l = new Listener(predicate);
        f_listListener.add(l);
        return l.f_future;
        }

    public void removeListener(Predicate<ProxyResponse> predicate)
        {
        Listener l = new Listener(predicate);
        f_listListener.remove(l);
        }

    // ----- inner class Listener -------------------------------------------

    protected static class Listener
        {
        public Listener(Predicate<ProxyResponse> predicate)
            {
            f_predicate = predicate;
            }

        protected void onNext(ProxyResponse proxyResponse)
            {

            ProxyResponse.ResponseCase type = proxyResponse.getResponseCase();
            if (!f_future.isDone()
                    && (type == ProxyResponse.ResponseCase.COMPLETE || type == ProxyResponse.ResponseCase.ERROR)
                    && f_predicate.evaluate(proxyResponse))
                {
                f_future.complete(proxyResponse);
                }
            }

        @Override
        public boolean equals(Object o)
            {
            if (o == null || getClass() != o.getClass()) return false;
            Listener listener = (Listener) o;
            return Objects.equals(f_predicate, listener.f_predicate);
            }

        @Override
        public int hashCode()
            {
            return Objects.hashCode(f_predicate);
            }

        // ----- data members -----------------------------------------------

        private final Predicate<ProxyResponse> f_predicate;

        private final CompletableFuture<ProxyResponse> f_future = new CompletableFuture<>();
        }

    // ----- data members ---------------------------------------------------

    private final List<Listener> f_listListener = new CopyOnWriteArrayList<>();
    }
