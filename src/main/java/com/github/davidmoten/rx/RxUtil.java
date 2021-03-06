package com.github.davidmoten.rx;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Utility methods for RxJava.
 */
public final class RxUtil {

	/**
	 * slf4j logger.
	 */
	private static final Logger log = LoggerFactory.getLogger(RxUtil.class);

	private RxUtil() {
		// prevent instantiation
	}

	/**
	 * Returns the concatenation of two {@link Observable}s but the first
	 * sequence will be emitted in its entirety and ignored before o2 starts
	 * emitting.
	 * 
	 * @param <T>
	 *            the generic type of the second observable
	 * @param o1
	 *            the sequence to ignore
	 * @param o2
	 *            the sequence to emit after o1 ignored
	 * @return observable result of concatenating two observables, ignoring the
	 *         first
	 */
	@SuppressWarnings("unchecked")
	public static <T> Observable<T> concatButIgnoreFirstSequence(
			Observable<?> o1, Observable<T> o2) {
		return Observable.concat((Observable<T>) o1.ignoreElements(), o2);
	}

	/**
	 * Logs errors and onNext at info level using slf4j {@link Logger}.
	 * 
	 * @param <T>
	 *            the return generic type
	 * @return a logging {@link Observer}
	 */
	public static <T> Observer<? super T> log() {
		return new Observer<T>() {

			@Override
			public void onCompleted() {
				// do nothing
			}

			@Override
			public void onError(Throwable e) {
				log.error(e.getMessage(), e);
			}

			@Override
			public void onNext(T t) {
				log.info(t + "");
			}
		};
	}

	/**
	 * Returns a constant value.
	 * 
	 * @param <R>
	 *            from type
	 * @param <S>
	 *            to type
	 * @param s
	 * @return a constant function with value s
	 */
	public static <R, S> Func1<R, S> constant(final S s) {
		return new Func1<R, S>() {
			@Override
			public S call(R t1) {
				return s;
			}
		};
	}

	/**
	 * Converts a transformation of an Observable into another Observable into
	 * an {@link Operator} suitable for use with
	 * {@link Observable#lift(Operator)} for instance.
	 * 
	 * @param <R>
	 *            from generic type
	 * @param <T>
	 *            to generic type
	 * @param operation
	 * @return an operator form of the given function
	 */
	public static <R, T> Operator<R, T> toOperator(
			Func1<Observable<T>, Observable<R>> operation) {
		return OperationToOperator.toOperator(operation);
	}

	/**
	 * Returns an {@link Action1} that increments a counter when the call method
	 * is called.
	 * 
	 * @param <T>
	 *            generic type of item being counted
	 * @return {@link Action1} to count calls.
	 */
	public static <T> CountingAction<T> counter() {
		return new CountingAction<T>();
	}

	public static class CountingAction<T> implements Action1<T> {
		private final AtomicLong count = new AtomicLong(0);

		public Observable<Long> count() {
			return Observable.create(new OnSubscribe<Long>() {

				@Override
				public void call(Subscriber<? super Long> subscriber) {
					subscriber.onNext(count.get());
					subscriber.onCompleted();
				}
			});
		}

		@Override
		public void call(T t) {
			count.incrementAndGet();
		}
	}

	public static <T extends Number> Func1<T, Boolean> greaterThanZero() {
		return new Func1<T, Boolean>() {

			@Override
			public Boolean call(T t) {
				return t.doubleValue() > 0;
			}
		};
	}

	/**
	 * Returns a {@link Func1} that returns an empty {@link Observable}.
	 * 
	 * @return
	 */
	public static <T> Func1<T, Observable<Object>> toEmpty() {
		return constant(Observable.<Object> empty());
	}

	/**
	 * Returns an {@link Operator} that flattens a sequence of
	 * {@link Observable} into a flat sequence of the items from the
	 * Observables. This operator may interleave the items asynchronously. For
	 * synchronous behaviour use {@link RxUtil#concat()}.
	 * 
	 * @return
	 */
	public static <T> Operator<T, Observable<T>> flatten() {
		return toOperator(new Func1<Observable<Observable<T>>, Observable<T>>() {

			@Override
			public Observable<T> call(Observable<Observable<T>> source) {
				return source.flatMap(RxUtil.<Observable<T>> identity());
			}
		});
	}

	public static <T> Func1<T, T> identity() {
		return new Func1<T, T>() {
			@Override
			public T call(T t) {
				return t;
			}
		};
	}

	public static <T> Operator<T, Observable<T>> concat() {
		return toOperator(new Func1<Observable<Observable<T>>, Observable<T>>() {

			@Override
			public Observable<T> call(Observable<Observable<T>> source) {
				return Observable.concat(source);
			}
		});
	}

}
