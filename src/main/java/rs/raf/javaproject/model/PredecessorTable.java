package rs.raf.javaproject.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.raf.javaproject.repository.Database;

import java.util.ArrayList;
import java.util.Collection;

@Data
@Component
public class PredecessorTable {

    @Autowired
    private Database database;

    private final ArrayList<Node> table;

    public void reconstructTable(){

        table.clear();
        ArrayList<Node> list = new ArrayList<>(database.getAllNodes().values());

        int myPos = list.indexOf(database.getInfo());

        int size = list.size();
        for (int step = 1; step < size; step *=2){

            int succPos = ((myPos - step) + size) % size;
            table.add(list.get(succPos));
        }

        System.out.println(database.getInfo().getId() + ":" + table);
    }

    public Collection<Node> broadcastingNodes(){
        ArrayList<Node> broadcastingNodes = new ArrayList<>();

        try {
            broadcastingNodes.add(table.get(0));
            broadcastingNodes.add(table.get(1));
            broadcastingNodes.add(table.get(2));
        }catch (IndexOutOfBoundsException e){

        }

        return broadcastingNodes;
    }
}
