


package wtf.faceac.penalty;
public interface ActionHandler {
    void handle(String command, PenaltyContext context);
    ActionType getActionType();
}