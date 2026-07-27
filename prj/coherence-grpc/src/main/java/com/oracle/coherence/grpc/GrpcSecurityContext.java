/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc;

import io.grpc.Context;

import java.security.PrivilegedAction;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import javax.security.auth.Subject;

/**
 * gRPC subject context helper.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.04
 */
public final class GrpcSecurityContext
    {
    /**
     * Return the current authenticated gRPC subject.
     *
     * @return the current subject, or {@code null}
     */
    public static Subject getCurrentSubject()
        {
        return KEY_SUBJECT.get();
        }

    /**
     * Return a context containing the specified subject.
     *
     * @param context  the source context
     * @param subject  the authenticated subject
     *
     * @return a context containing the subject
     */
    public static Context withSubject(Context context, Subject subject)
        {
        return context.withValue(KEY_SUBJECT, subject);
        }

    /**
     * Run an action as the specified subject.
     *
     * @param subject  the subject
     * @param action   the action to run
     */
    public static void runAs(Subject subject, Runnable action)
        {
        Objects.requireNonNull(action, "action");
        if (subject == null)
            {
            action.run();
            }
        else
            {
            Subject.doAs(subject, (PrivilegedAction<Void>) () ->
                {
                action.run();
                return null;
                });
            }
        }

    /**
     * Run a supplier as the specified subject.
     *
     * @param subject   the subject
     * @param supplier  the supplier
     * @param <T>       the supplied type
     *
     * @return the supplied value
     */
    public static <T> T supplyAs(Subject subject, Supplier<T> supplier)
        {
        Objects.requireNonNull(supplier, "supplier");
        if (subject == null)
            {
            return supplier.get();
            }
        return Subject.doAs(subject, (PrivilegedAction<T>) supplier::get);
        }

    /**
     * Wrap an executor so scheduled work preserves the current gRPC context
     * and runs under the authenticated subject contained in that context.
     *
     * @param executor  the delegate executor
     *
     * @return the context-aware executor
     */
    public static Executor contextAware(Executor executor)
        {
        Objects.requireNonNull(executor, "executor");
        return command ->
            {
            Context context = Context.current();
            contextAware(context, executor).execute(command);
            };
        }

    /**
     * Wrap an executor so scheduled work preserves the specified gRPC context
     * and runs under the authenticated subject contained in that context.
     *
     * @param context   the context to restore
     * @param executor  the delegate executor
     *
     * @return the context-aware executor
     */
    public static Executor contextAware(Context context, Executor executor)
        {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(executor, "executor");
        return command -> executor.execute(() -> runInContext(context, command));
        }

    /**
     * Run the specified command under the specified gRPC context.
     *
     * @param context  the context to restore
     * @param command  the command to run
     */
    public static void runInContext(Context context, Runnable command)
        {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(command, "command");

        Context previous = context.attach();
        try
            {
            runAs(KEY_SUBJECT.get(), command);
            }
        finally
            {
            context.detach(previous);
            }
        }

    /**
     * gRPC context key for authenticated subjects.
     */
    public static final Context.Key<Subject> KEY_SUBJECT =
            Context.key("com.oracle.coherence.grpc.subject");

    private GrpcSecurityContext()
        {
        }
    }
