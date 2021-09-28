package commands;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import collection.*;
public class CommandGroupCounting extends AbstractCommand {

    public CommandGroupCounting(MyCollection myCollection) {
        super(myCollection);
    }

    @Override
    public String execute() {
        if (!super.getCollection().checkToEmpty()) {
            Map<LocalDateTime, Integer> LocalDateMap = super.getCollection().groupByCreationDate();
            Set<LocalDateTime> keys = LocalDateMap.keySet();
            StringBuilder stringBuilder = new StringBuilder();
            for (LocalDateTime key : keys) {
                stringBuilder.append("Creation date is " + key + " .The amounts is " + LocalDateMap.get(key)+"\n");
            }
            return stringBuilder.toString();
        } else {
            return "Collection is empty.Add elements firstly";
        }
    }
}
