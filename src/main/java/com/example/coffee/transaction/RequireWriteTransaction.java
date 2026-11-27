package com.example.coffee.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Requires an existing write transaction; never starts or independently commits one here.
 * IdempotencyService owns transaction start, commit and rollback through TransactionTemplate.
 *
 * <p>Apply to public Spring bean methods invoked through an injected proxy. Direct construction,
 * private methods and self-invocation bypass the proxy. Read-only checks require the transaction
 * manager's validateExistingTransaction=true setting.
 *
 * <p>A runtime exception marks the participating transaction rollback-only. Catching it does not
 * permit the caller to commit earlier writes.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Transactional(propagation = Propagation.MANDATORY, readOnly = false)
public @interface RequireWriteTransaction {}
