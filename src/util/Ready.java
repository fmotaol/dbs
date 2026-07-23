package util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Ready<T> implements Future<T> {

	private T obj;
	
	public Ready(T obj) {
		super();
		this.obj = obj;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new RuntimeException("Chamada inválida");
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return obj;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return get();
	}

}
