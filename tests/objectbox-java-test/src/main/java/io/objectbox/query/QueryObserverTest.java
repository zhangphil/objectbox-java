package io.objectbox.query;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.TestEntity;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataTransformer;


import static io.objectbox.TestEntity_.simpleInt;
import static org.junit.Assert.assertEquals;

public class QueryObserverTest extends AbstractObjectBoxTest implements DataObserver<List<TestEntity>> {

    private Box<TestEntity> box;
    private List<List<TestEntity>> receivedChanges = new CopyOnWriteArrayList<>();
    private CountDownLatch latch = new CountDownLatch(1);

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    @Test
    public void testObserver() {
        int[] valuesInt = {2003, 2007, 2002};
        Query<TestEntity> query = box.query().in(simpleInt, valuesInt).build();
        assertEquals(0, query.count());

        query.subscribe().observer(this);
        assertLatchCountedDown(latch, 5);
        assertEquals(1, receivedChanges.size());
        assertEquals(0, receivedChanges.get(0).size());

        receivedChanges.clear();
        latch = new CountDownLatch(1);
        putTestEntitiesScalars();
        assertLatchCountedDown(latch, 5);

        assertEquals(1, receivedChanges.size());
        assertEquals(3, receivedChanges.get(0).size());
    }

    @Test
    public void testSingle() throws InterruptedException {
        putTestEntitiesScalars();
        int[] valuesInt = {2003, 2007, 2002};
        Query<TestEntity> query = box.query().in(simpleInt, valuesInt).build();
        query.subscribe().single().observer(this);
        assertLatchCountedDown(latch, 5);
        assertEquals(1, receivedChanges.size());
        assertEquals(3, receivedChanges.get(0).size());

        receivedChanges.clear();
        putTestEntities(1);
        Thread.sleep(20);
        assertEquals(0, receivedChanges.size());
    }

    @Test
    public void testTransformer() throws InterruptedException {
        int[] valuesInt = {2003, 2007, 2002};
        Query<TestEntity> query = box.query().in(simpleInt, valuesInt).build();
        assertEquals(0, query.count());
        final List<Integer> receivedSums = new ArrayList<>();

        query.subscribe().transform(new DataTransformer<List<TestEntity>, Integer>() {

            @Override
            public Integer transform(List<TestEntity> source) throws Exception {
                int sum = 0;
                for (TestEntity entity : source) {
                    sum += entity.getSimpleInt();
                }
                return sum;
            }
        }).observer(new DataObserver<Integer>() {
            @Override
            public void onData(Integer data) {
                receivedSums.add(data);
                latch.countDown();
            }
        });
        assertLatchCountedDown(latch, 5);

        latch = new CountDownLatch(1);
        putTestEntitiesScalars();
        assertLatchCountedDown(latch, 5);
        Thread.sleep(20);

        assertEquals(2, receivedSums.size());
        assertEquals(0, (int) receivedSums.get(0));
        assertEquals(2003 + 2007 + 2002, (int) receivedSums.get(1));
    }

    private List<TestEntity> putTestEntitiesScalars() {
        return putTestEntities(10, null, 2000);
    }

    @Override
    public void onData(List<TestEntity> queryResult) {
        receivedChanges.add(queryResult);
        latch.countDown();
    }
}
