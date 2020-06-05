package org.example.reacurringTasks;

import lombok.Data;
import org.example.model.Point;
import org.example.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Component
public class GameOfChaos implements Runnable{

    DatabaseService databaseService;

    private AtomicBoolean pause;

    @Override
    public void run() {
        while(true){
            if(pause.get())
                sleep(1000);
            else
                doChaos();
        }
    }

    private void doChaos() {
        sleep(1000);
        System.out.println(databaseService);
        System.out.println(databaseService.getMyRegion());
        Point tracepoint = databaseService.getMyRegion().getTracepoint();
        Double proportion = databaseService.getMyRegion().getProportion();
        Point randomPoint = randomStartingPoint();
        Point newPoint = new Point(
                tracepoint.getX() + proportion * (randomPoint.getX() - tracepoint.getX()),
                tracepoint.getY() + proportion * (randomPoint.getY() - tracepoint.getY()));

        databaseService.saveData(newPoint);
        databaseService.getMyRegion().setTracepoint(tracepoint);
    }

    private Point randomStartingPoint(){
        Random r = new Random();
        return databaseService.getMyRegion().getStartingPoints().get(databaseService.getMyRegion().getStartingPoints().size());
    }

    private void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
        }
    }
}
