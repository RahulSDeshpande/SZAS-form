package com.szas.sync;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class ContentObserverProviderImplTest {
	protected ContentObserverProviderImpl contentObserverProviderImpl;
	protected class MyContentObserver implements ContentObserver {
		public boolean notiffied = false;

		public MyContentObserver() {
		}

		@Override
		public void onChange(boolean whileSync) {
			notiffied = true;
		}
		
	}
	@Before
	public void setUp() {
		contentObserverProviderImpl = new ContentObserverProviderImpl();
	}
	
	@Test
	public void testNotification() {
		MyContentObserver contentObserver1 = new MyContentObserver();
		MyContentObserver contentObserver2 = new MyContentObserver();
		contentObserverProviderImpl.addContentObserver(contentObserver1);
		contentObserverProviderImpl.addContentObserver(contentObserver2);
		contentObserverProviderImpl.notifyContentObservers(false);
		assertTrue("Content obserer schould be notified", contentObserver1.notiffied);
		assertTrue("Content obserer schould be notified", contentObserver2.notiffied);
	}
	
	@Test
	public void testRemove() {
		MyContentObserver contentObserver1 = new MyContentObserver();
		MyContentObserver contentObserver2 = new MyContentObserver();
		contentObserverProviderImpl.addContentObserver(contentObserver1);
		contentObserverProviderImpl.addContentObserver(contentObserver2);
		boolean removed = contentObserverProviderImpl.removeContentObserver(contentObserver1);
		assertTrue("return value schould be true", removed);
		contentObserverProviderImpl.notifyContentObservers(false);
		assertFalse("Removed content obserer schould not be notified", contentObserver1.notiffied);
		assertTrue("Content obserer schould be notified", contentObserver2.notiffied);
	}
	@Test
	public void testRemoveUnadded() {
		MyContentObserver contentObserver1 = new MyContentObserver();
		MyContentObserver contentObserver2 = new MyContentObserver();
		contentObserverProviderImpl.addContentObserver(contentObserver1);
		boolean removed = contentObserverProviderImpl.removeContentObserver(contentObserver2);
		assertFalse("Removing not added content observer schould not succesed", removed);
		contentObserverProviderImpl.notifyContentObservers(false);
		assertTrue("Content obserer schould be notified", contentObserver1.notiffied);
	}
}
