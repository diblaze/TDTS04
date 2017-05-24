import ChatApp.*;          // The package containing our stubs. 
import org.omg.CosNaming.*; // HelloServer will use the naming service.

import java.util.HashMap;
import java.util.Set;

import org.omg.CORBA.*;     // All CORBA applications need these classes. 
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;

class ChatImpl extends ChatPOA {

    //Gamepart
    private final int boardSizeX = 9;
    private final int boardSizeY = 9;
    //FourInRow Logic
    private FourInRowLogic gameLogic;
    private ORB orb;
    //All users in server
    private HashMap<ChatCallback, String> users = new HashMap<ChatCallback, String>();

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public void play(ChatCallback user, String team) {
        team = team.toLowerCase();
        team = team.trim();
        if (team.length() == 0) {
            return;
        }
        if (isActiveUser(user)) {

            if (gameLogic == null) {
                gameLogic = new FourInRowLogic(10, 10);
            } else if (!gameLogic.isGameRunning()) {
                gameLogic.newGame();
            }

            if (gameLogic.GetAmountOfPlayers() == 0) {
                if (gameLogic.joinUser(user, team)) {
                    user.callback("Joined the game! Waiting for one more player.");
                } else {
                    user.callback("Couldn't join the game!");
                    return;
                }
            } else if (gameLogic.GetAmountOfPlayers() == 1) {
                if (gameLogic.joinUser(user, team)) {
                    user.callback("Joined the game! Game is starting!");
                    Set<ChatCallback> playersInGame = gameLogic.getPlayers();
                    for (ChatCallback player : playersInGame) {
                        player.callback("Team X starts!");
                    }
                    if (!gameLogic.isGameRunning()) {
                        gameLogic.newGame();
                        sendAll("Team X starts!");
                    }
                } else {
                    user.callback("Couldn't join the game! Make sure you pick the empty team!");
                    return;
                }
            } else if (gameLogic.GetAmountOfPlayers() > 2) {
                if (gameLogic.joinUser(user, team)) {
                    user.callback("Joined a game in progress!");
                } else {
                    user.callback("Couldn't join the game!");
                    return;
                }
            }
            draw(user);

        } else {
            user.callback("You need to be an active user to do that...");
        }
    }

    public void put(ChatCallback user, String column) {
        if (!isActiveUser(user)) {
            user.callback("You need to be an active user to do this!");
            return;
        } else if (!gameLogic.isPlayerInGame(user)) {
            user.callback("You need to be in the game to do this!");
            return;
        } else if (!gameLogic.isGameRunning()) {
            user.callback("Game is not running!");
            return;
        }

        column = column.trim().toLowerCase();
        System.out.println(column);

        if (!gameLogic.putXO(user, column)) {
            user.callback("You can not put in that column!");
            return;
        }

        //check game over
        if (gameLogic.isGameOver()) {
            String team = gameLogic.getTeamOf(user);
            drawToPlayers();
            sendAll("Team " + team + " won!");
        } else {
            drawToPlayers();
        }


    }


    public void drawToPlayers() {
        Set<ChatCallback> playersInGame = gameLogic.getPlayers();
        String[] outputs = gameLogic.getBoard();

        for (ChatCallback player : playersInGame) {
            player.callback(outputs[0]);
            player.callback(outputs[1]);
        }
    }

    public void draw(ChatCallback user) {
        //get the strings to write back to user
        if (!isActiveUser(user)) {
            user.callback("You need to be an active user to see the board!");
            return;
        }

        String[] outputs = gameLogic.getBoard();

        user.callback(outputs[0]);
        user.callback(outputs[1]);

    }

    public void post(ChatCallback user, String msg) {
        if (isActiveUser(user)) {
            sendAll(users.get(user) + " said: " + msg);
            System.out.println(users.get(user) + "-> " + msg);
        } else {
            user.callback("You need to be an active user to do that...");
        }

    }

    public void join(ChatCallback user, String name) {
        if (name.isEmpty()) {
            user.callback("Sorry, you must have a valid name!");
        } else {
            if (isActiveUser(user)) {
                user.callback("You are an active user...");
                System.out.println("Are in list: " + name);
            } else if (users.containsValue(name)) {
                user.callback("Sorry that name is taken...");
                System.out.println("Username taken: " + name);
            } else {
                users.put(user, name);
                sendAll(name + " has joined...");
                user.callback("Wecome " + name);
                System.out.println("Adding user " + name);
            }
        }

    }

    public void leave(ChatCallback user) {
        if (isActiveUser(user)) {
            user.callback("Bye bye");
            sendAll(users.get(user) + " left!");
            users.remove(user);
        } else {
            user.callback("You need to be an active user to do that...");
        }
    }

    public void list(ChatCallback requester) {
        if (isActiveUser(requester)) {
            String allUsers = "List of registered users:";
            for (ChatCallback user : users.keySet()) {
                allUsers += "\n" + users.get(user);
            }
            requester.callback(allUsers);
        } else {
            requester.callback("You need to be an active user to do that...");
        }
    }

    private boolean isActiveUser(ChatCallback user) {
        return users.containsKey(user);
    }

    private void sendAll(String message) {
        for (ChatCallback user : users.keySet()) {
            user.callback(message);
        }
    }
}

public class ChatServer {
    public static void main(String args[]) {
        try {
            // create and initialize the ORB
            ORB orb = ORB.init(args, null);

            // create servant (impl) and register it with the ORB
            ChatImpl chatImpl = new ChatImpl();
            chatImpl.setORB(orb);

            // get reference to rootpoa & activate the POAManager
            POA rootpoa =
                    POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();

            // get the root naming context
            org.omg.CORBA.Object objRef =
                    orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // obtain object reference from the servant (impl)
            org.omg.CORBA.Object ref =
                    rootpoa.servant_to_reference(chatImpl);
            Chat cref = ChatHelper.narrow(ref);

            // bind the object reference in naming
            String name = "Chat";
            NameComponent path[] = ncRef.to_name(name);
            ncRef.rebind(path, cref);

            // Application code goes below
            System.out.println("ChatServer ready and waiting ...");

            // wait for invocations from clients
            orb.run();
        } catch (Exception e) {
            System.err.println("ERROR : " + e);
            e.printStackTrace(System.out);
        }

        System.out.println("ChatServer Exiting ...");
    }

}


class FourInRowLogic {
    private static char[][] board;
    //All players
    private HashMap<ChatCallback, String> players = new HashMap<>();
    private int width;
    private int height;
    private int bottomRow;
    private String teamTurn = "x";
    private int lastPawnX;
    private int lastPawnY;
    private boolean running = false;


    public FourInRowLogic(int _width, int _height) {
        width = _width;
        height = _height;
        bottomRow = height - 1;

        board = new char[width][height];
    }

    private static boolean contains(String haystack, String needle) {
        return haystack.contains(needle);
    }

    public void newGame() {
        createBoard();
        teamTurn = "x";

        running = true;
    }

    public boolean isPlayerInGame(ChatCallback user) {
        return players.get(user) != null;
    }

    public int GetAmountOfPlayers() {
        return players.size();
    }

    public boolean joinUser(ChatCallback user, String team) {

        if (players.get(user) != null) {
            return false;
        }

        if (GetAmountOfPlayers() == 0) {
            players.put(user, team);
            return true;
        }

        //make sure the teams have at least one player in each team
        //before allowing players to choose teams freely.
        int countTeamX = 0;
        int countTeamO = 0;
        for (ChatCallback player : players.keySet()) {
            System.out.println(players.get(player));
            if (players.get(player).equals("x")) {
                ++countTeamX;
            } else if (players.get(player).equals("o")) {
                ++countTeamO;
            }
        }

        System.out.println(countTeamX);
        System.out.println(countTeamO);
        //if player tries to join non-empty team X but team O is empty.
        if (team.equals("x") && countTeamX == 1 && countTeamO == 0) {
            return false;
        }
        //if players tries to join non-empty team O but team X is empty.
        else if (team.equals("o") && countTeamO == 1 && countTeamX == 0) {
            return false;
        }

        //otherwise let play join team.
        players.put(user, team);
        return true;


    }

    public Set<ChatCallback> getPlayers() {
        return players.keySet();
    }

    public boolean isGameRunning() {
        return running;
    }

    public boolean putXO(ChatCallback user, String column) {


        if (!isGameRunning()) {
            return false;
        }

        //intify the column in parameter (don't hate us for doing this)
        int columnToPut = Integer.parseInt(column) - 1;

        //counter for height
        int heightCounter = 0;

        //return if it is not users turn
        if (!players.get(user).equals(teamTurn)) {
            System.out.println("Not your turn!");
            return false;
        }
        while (true) {
            if (columnToPut > width) {
                System.out.println("Can't put outside of width");
                return false;
            }

            if (board[bottomRow - heightCounter][columnToPut] == ' '
                    && board[bottomRow - heightCounter][columnToPut] != 'x'
                    && board[bottomRow - heightCounter][columnToPut] != 'o') {
                board[bottomRow - heightCounter][columnToPut] = teamTurn.charAt(0);
                nextTeam();
                lastPawnX = columnToPut;
                lastPawnY = bottomRow - heightCounter;
                return true;

            }

            ++heightCounter;
            if (heightCounter == width) {
                System.out.println("Can't put too high!");
                return false;
            }
        }
    }

    public String getTeamOf(ChatCallback user) {
        return players.get(user);
    }

    public boolean isGameOver() {

        //won?
        boolean gameOver = false;

        //last played pawn
        char pawn = board[lastPawnY][lastPawnX];

        //streak = xxxx or oooo
        String streak = String.format("%c%c%c%c", pawn, pawn, pawn, pawn);


        //check horizontal
        if (contains(horizontal(), streak)) {
            gameOver = true;
        } else if (contains(vertical(), streak)) {
            gameOver = true;
        } else if (contains(rightDiagonal(), streak) || contains(leftDiagonal(), streak)) {
            gameOver = true;
        }

        if (gameOver) {
            running = false;
        }

        return gameOver;

    }

    private String horizontal() {
        return new String(board[lastPawnY]);
    }

    private String vertical() {
        StringBuilder sb = new StringBuilder(height);
        for (int h = 0; h < height; h++) {
            sb.append(board[h][lastPawnX]);
        }
        return sb.toString();
    }

    //Give contents of the "/" containing the last played pawn
    private String rightDiagonal() {
        StringBuilder sb = new StringBuilder(this.height);
        for (int h = 0; h < height; h++) {
            int w = lastPawnX + lastPawnY - h;
            if (0 <= w && w < width) {
                sb.append(board[h][w]);
            }
        }
        return sb.toString();
    }

    //Give contents of the "\" containing the last played pawn
    private String leftDiagonal() {
        StringBuilder sb = new StringBuilder(this.height);
        for (int h = 0; h < height; h++) {
            int w = lastPawnX - lastPawnY + h;
            if (0 <= w && w < width) {
                sb.append(board[h][w]);
            }
        }
        return sb.toString();
    }

    private void nextTeam() {
        if (!isGameRunning()) {
            return;
        }

        if (teamTurn.equals("x")) {
            teamTurn = "o";
        } else {
            teamTurn = "x";
        }
    }

    private void createBoard() {
        for (int w = 0; w < width; ++w) {
            for (int h = 0; h < height; ++h) {
                board[w][h] = ' ';
            }
        }
    }

    public String[] getBoard() {
        //needed because we are sending a string to clients.
        String boardToPrint = "";

        for (int y = 0; y < height; ++y) {
            boardToPrint += "\n  |";
            for (int x = 0; x < width; ++x) {
                switch (board[y][x]) {
                    case 'x':
                        boardToPrint += "x";
                        break;
                    case 'o':
                        boardToPrint += "o";
                        break;
                    default:
                        boardToPrint += " ";
                }
                boardToPrint += "|";
            }
        }

        String columns = "\n ";
        for (int i = 1; i <= width; ++i) {
            columns += " " + Integer.toString(i);
        }

        return new String[]{boardToPrint, columns};

    }
}