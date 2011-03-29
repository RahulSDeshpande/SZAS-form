package com.szas.server.gwt.client;
import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

public class SyncHelpersTest {
	private class MockSubTuple extends Tuple {
		public int data = 0;
		public void assertSame(MockSubTuple otherTuple) {
			assertEquals("Data schould be the same", this.data, otherTuple.data);
			assertEquals("Index schould be the same", this.getId(), otherTuple.getId());
		}
	}
	private static final int EXAMPLE_DATA = 10;
	private static final int NEW_EXAMPLE_DATA = 12;
	LocalSyncHelper localSyncHelper;
	RemoteSyncHelper remoteSyncHelper;
	RemoteDAO<MockSubTuple> remoteMockTuples;
	private LocalDAO<MockSubTuple> localMockTuples;
	@Before
	public void setUp() {
		remoteSyncHelper = new RemoteSyncHelperImpl();
		localSyncHelper = new LocalSyncHelperImpl(new SyncLocalService() {
			
			@Override
			public void sync(ArrayList<ToSyncElementsHolder> toSyncElementsHolders,
					SyncLocalServiceResult callback) {
				Void result = remoteSyncHelper.sync(toSyncElementsHolders);
				callback.onSuccess(result);
			}
		});
		
		localMockTuples = new LocalDAOImpl<MockSubTuple>();
		remoteMockTuples = new RemoteDAOImpl<MockSubTuple>();
		
		localSyncHelper.append(MockSubTuple.class, localMockTuples);
		remoteSyncHelper.append(MockSubTuple.class, remoteMockTuples);
	}
	@Test
	public void testPushing() {
		MockSubTuple localTuple = makeExampleTuple();
		localMockTuples.insert(localTuple);
		assertEquals("Size after insertion schould be 1",1,localMockTuples.getAll().size());
		
		localSyncHelper.sync();
		
		assertEquals("Size after sync schould be same" ,1,localMockTuples.getAll().size());
		
		assertEquals("Size after sync schould be equal",
				localMockTuples.getAll().size(),
				remoteMockTuples.getAll().size());
		
		ArrayList<MockSubTuple> remoteTuples = remoteMockTuples.getAll();
		MockSubTuple remoteTuple = remoteTuples.get(0);
		localTuple.assertSame(remoteTuple);	
		
		remoteTuple.data = NEW_EXAMPLE_DATA;
		remoteMockTuples.update(remoteTuple);
		
		localSyncHelper.sync();
		
		ArrayList<MockSubTuple> localTuples = localMockTuples.getAll();
		assertEquals("(sync after update schould not lead to create new rows)",1,localTuples.size());
		remoteTuple.assertSame(localTuple);
		
	}
	@Test
	public void testGetting() {
		// TODO create test for getting data
	}
	private MockSubTuple makeExampleTuple() {
		MockSubTuple newTuple = new MockSubTuple();
		newTuple.data = EXAMPLE_DATA;
		return newTuple;
	}
}
