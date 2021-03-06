package command;

public class Command {
    private final String name;
    private final String description;
    private final Declarations decls;
    private final Line line;

    public Command(String name, String desciption, Declarations decls, Line line) {
        this.name = name;
        this.description = desciption;
        this.decls = decls;
        this.line = line;
    }

    public void run() {
        String cmd;
        Console console;

        if (decls.runFrontend(name, description)) {
            cmd = line.eval();
            console = new Console();
            console.execute(cmd);
        } else {
            // dialog canceled - do nothing
        }
    }
}
