package com.example.trylma2;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.example.trylma2.Server.players;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Kontroler obsługujący logikę gry dla serwera Trylmy.
 */
public class ServerController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private Label activePlayerLabel;

    @FXML
    private Rectangle playerColorBox;

    @FXML
    public TextField moveInputField;

    @FXML
    public TextField readedGameField;

    @FXML
    private Button submitMoveButton;

    @FXML
    private Button passTurnButton;

    @FXML
    private Button BotButton;

    @FXML
    private Button DBButton;

    @FXML
    private Button checkWinnerButton;

    /**
     * Identyfikator obecnie aktywnego gracza.
     */
    public int currentActivePlayer = 1;

    /**
     * Plansza gry, reprezentowana jako tablica dwuwymiarowa.
     */
    public int[][] board;

    /**
     * Tablica pomocnicza do sprawdzania warunków zwycięstwa.
     */
    public int[][] check;

    /**
     * Tablica przechowująca identyfikatory zwycięzców.
     */
    int[] winners;

    /**
     * Indeks określający miejsce w rankingu zwycięzców.
     */
    int place = 1;

    /**
     * Flaga określająca, czy gra odbywa się w trybie szybkiej rozgrywki.
     */
    public boolean szybkie = false;

    /**
     * Metoda inicjalizująca komponenty po załadowaniu widoku FXML.
     */
    @FXML
    public void initialize() {
        if (rootPane == null) {
            System.err.println("rootPane is not initialized!");
        }
    }

    int w;
    int y = 0;

    /**
     * Obsługuje wybór liczby graczy z menu kontekstowego.
     *
     * @param event Zdarzenie wyboru opcji.
     */
    @FXML
    public void onPlayerCountSelected(javafx.event.ActionEvent event) {
        MenuItem selectedMenuItem = (MenuItem) event.getSource();
        int playernumbers = 2;
        try {
            playernumbers = Integer.parseInt(selectedMenuItem.getText());
        } catch (NumberFormatException e) {
            y = 1;
        }
        Random random = new Random();
        int playerCount = playernumbers;
        w = playerCount;
        currentActivePlayer = random.nextInt(playerCount) + 1;
        Platform.runLater(() -> updateActivePlayer(currentActivePlayer));
        new Thread(() -> {
            Server.startServer(playerCount);
        }).start();

        String type = selectedMenuItem.getText();
        BoardView boardView = new BoardView();
        this.board = boardView.initializeBoard(type);
        this.check = boardView.CheckWinnerBoard(type);
        AnchorPane boardPane = boardView.createBoardPane(type);
        winners = new int[playerCount + 1];
        rootPane.getChildren().clear();
        rootPane.getChildren().addAll(boardPane, activePlayerLabel, playerColorBox, moveInputField, submitMoveButton, passTurnButton, checkWinnerButton, BotButton, readedGameField, DBButton);
    }

    ApplicationContext context = new ClassPathXmlApplicationContext("Beans.xml");
    JdbcTemplate gameTemplate = (JdbcTemplate) context.getBean("iteratorTemplate");

    public int getMaxGameId() {
        String sql = "SELECT MAX(id) FROM iterator";
        Integer maxId = gameTemplate.queryForObject(sql, Integer.class);
        return (maxId != null) ? maxId : 0;
    }

    int game = getMaxGameId() + 1;
    private JdbcTemplate jdbcTemplate = (JdbcTemplate) context.getBean("jdbcTemplate");

    public void saveMove(String move, int activePlayer) {
        String sql = "INSERT INTO moves (move, active_player, game) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, move, activePlayer, game);
    }

    public void doMove(String move) {
        System.out.println("Ruch: " + move + " przesłany.");
        for (Server.Player player : players) {
            player.out.println("Gracz " + currentActivePlayer + " wykonał ruch " + move);
        }
        String[] parts = move.split(" ");
        int x1 = Integer.parseInt(parts[0]);
        int y1 = Integer.parseInt(parts[1]);
        int x2 = Integer.parseInt(parts[2]);
        int y2 = Integer.parseInt(parts[3]);

        board[x1][y1] = 0;
        board[x2][y2] = currentActivePlayer;

        Platform.runLater(() -> {
            GridPane boardPane = BoardView.createGrid(board);
            moveInputField.clear();
            rootPane.getChildren().clear();
            rootPane.getChildren().addAll(boardPane, activePlayerLabel, playerColorBox, moveInputField, submitMoveButton, passTurnButton, checkWinnerButton, BotButton, readedGameField, DBButton);
            if (!jump) {
                updateToNextActivePlayer();
                jump = false;
            }
        });
    }

    @FXML
    public void onBotMove() {
        int firstX = 0, firstY = 0;
        if (currentActivePlayer == 1)//dół:G1
        {
            outerLoop:
            for (int i = 0; i < 17; i++) {
                for (int j = 0; j < 17; j++) {
                    if (board[i][j] == currentActivePlayer) {
                        firstX = i;
                        firstY = j;
                        String[] possibleMoves = {
                                firstX + " " + firstY + " " + (firstX + 2) + " " + (firstY + 2),
                                firstX + " " + firstY + " " + (firstX + 2) + " " + firstY,
                                firstX + " " + firstY + " " + (firstX + 1) + " " + (firstY + 1),
                                firstX + " " + firstY + " " + (firstX + 1) + " " + firstY,
                                firstX + " " + firstY + " " + firstX + " " + (firstY + 1),
                        };
                        for (String move : possibleMoves) {
                            if (validateStandardMove(move)) {
                                doMove(move);
                                jump = false;
                                saveMove(move, currentActivePlayer);
                                break outerLoop;
                            }
                        }

                    }

                }
            }
        }
        if ((w == 2 && currentActivePlayer == 2 && y == 0) || (w == 4 && currentActivePlayer == 3) || (w == 6 && currentActivePlayer == 4))//góra:2pG2,4pG3, 6pG4
        {
            outerLoop:
            for (int i = 16; i >= 0; i--) {
                for (int j = 16; j >= 0; j--) {
                    if (board[i][j] == currentActivePlayer) {
                        firstX = i;
                        firstY = j;
                        String[] possibleMoves = {
                                firstX + " " + firstY + " " + (firstX - 2) + " " + (firstY - 2),
                                firstX + " " + firstY + " " + (firstX - 2) + " " + firstY,
                                firstX + " " + firstY + " " + (firstX - 1) + " " + (firstY - 1),
                                firstX + " " + firstY + " " + (firstX - 1) + " " + firstY,
                                firstX + " " + firstY + " " + firstX + " " + (firstY - 1)
                        };

                        for (String move : possibleMoves) {
                            if (validateStandardMove(move)) {
                                doMove(move);
                                jump = false;
                                saveMove(move, currentActivePlayer);
                                break outerLoop;
                            }
                        }
                    }

                }
            }
        }
        if ((w == 6 && currentActivePlayer == 6))//prawo: 6pG6
        {
            outerLoop:
            for (int i = 0; i < 17; i++) {
                for (int j = 0; j < 17; j++) {
                    if (board[j][i] == currentActivePlayer) {
                        firstX = j;
                        firstY = i;
                        String[] possibleMoves = {
                                firstX + " " + firstY + " " + (firstX + 2) + " " + (firstY + 2),
                                firstX + " " + firstY + " " + (firstX) + " " + (firstY + 2),
                                firstX + " " + firstY + " " + (firstX + 1) + " " + (firstY + 1),
                                firstX + " " + firstY + " " + firstX + " " + (firstY + 1),
                                firstX + " " + firstY + " " + (firstX + 1) + " " + firstY
                        };

                        for (String move : possibleMoves) {
                            if (validateStandardMove(move)) {
                                doMove(move);
                                jump = false;
                                saveMove(move, currentActivePlayer);
                                break outerLoop;
                            }
                        }
                    }

                }
            }
        }
        if ((w == 3 && currentActivePlayer == 3) || (w == 4 && currentActivePlayer == 4) || (w == 6 && currentActivePlayer == 5))//prawyskos: 3pG3, 4pG4, 6pG5
        {
            outerLoop:
            for (int i = 0; i < 17; i++) {
                for (int j = 0; j < 17; j++) {
                    if (board[j][i] == currentActivePlayer) {
                        firstX = j;
                        firstY = i;
                        String[] possibleMoves = {
                                firstX + " " + firstY + " " + (firstX - 2) + " " + firstY,
                                firstX + " " + firstY + " " + (firstX) + " " + (firstY + 2),
                                firstX + " " + firstY + " " + (firstX - 1) + " " + firstY,
                                firstX + " " + firstY + " " + firstX + " " + (firstY + 1)

                        };

                        for (String move : possibleMoves) {
                            if (validateStandardMove(move)) {
                                doMove(move);
                                jump = false;
                                saveMove(move, currentActivePlayer);
                                break outerLoop;
                            }
                        }
                    }

                }
            }
        }
        if ((w == 3 && currentActivePlayer == 2) || (w == 6 && currentActivePlayer == 3))//lewo: 3pG2 ,6pG3
        {
            outerLoop:
            for (int i = 16; i >= 0; i--) {
                for (int j = 16; j >= 0; j--) {
                    if (board[j][i] == currentActivePlayer) {
                        firstX = j;
                        firstY = i;
                        String[] possibleMoves = {
                                firstX + " " + firstY + " " + (firstX - 2) + " " + (firstY - 2),
                                firstX + " " + firstY + " " + (firstX) + " " + (firstY - 2),
                                firstX + " " + firstY + " " + (firstX - 1) + " " + (firstY - 1),
                                firstX + " " + firstY + " " + firstX + " " + (firstY - 1),
                                firstX + " " + firstY + " " + (firstX - 1) + " " + firstY
                        };

                        for (String move : possibleMoves) {
                            if (validateStandardMove(move)) {
                                doMove(move);
                                jump = false;
                                saveMove(move, currentActivePlayer);
                                break outerLoop;
                            }
                        }
                    }

                }
            }
        }
        if ((y == 1 && currentActivePlayer == 2) || (w == 4 && currentActivePlayer == 2) || (w == 6 && currentActivePlayer == 2))//lewyskos: ygG2, 4pG2, 6pG2
        {
            outerLoop:
            for (int i = 16; i >= 0; i--) {
                for (int j = 16; j >= 0; j--) {
                    if (board[j][i] == currentActivePlayer) {
                        firstX = j;
                        firstY = i;
                        String[] possibleMoves = {
                                firstX + " " + firstY + " " + (firstX + 2) + " " + (firstY),
                                firstX + " " + firstY + " " + (firstX) + " " + (firstY - 2),
                                firstX + " " + firstY + " " + (firstX + 1) + " " + (firstY),
                                firstX + " " + firstY + " " + firstX + " " + (firstY - 1),
                        };

                        for (String move : possibleMoves) {
                            if (validateStandardMove(move)) {
                                doMove(move);
                                jump = false;
                                saveMove(move, currentActivePlayer);
                                break outerLoop;
                            }
                        }
                    }

                }
            }
        }
    }

    public List<Map<String, Object>> getMovesForGame() {
        int readedGame;
        try {
            readedGame = Integer.parseInt(readedGameField.getText());
        } catch (NumberFormatException e) {
            System.out.println("Nieprawidłowy format ID gry.");
            return Collections.emptyList();
        }
        String sql = "SELECT move, active_player FROM moves WHERE game = ?";
        return jdbcTemplate.queryForList(sql, readedGame);
    }

    @FXML
    public void onDBMove() {
        List<Map<String, Object>> moves = getMovesForGame();
        if (moves.isEmpty()) {
            System.out.println("Brak ruchów dla tej gry.");
            return;
        }
        for (Map<String, Object> moveData : moves) {
            String move = (String) moveData.get("move");
            int activePlayer = (int) moveData.get("active_player");
            currentActivePlayer = activePlayer;
            doMove(move);
        }
    }

    @FXML
    public void onSubmitMove() {
        String move = moveInputField.getText();
        boolean validate;
        if (szybkie)
            validate = validateSpeedMove(move);
        else
            validate = validateStandardMove(move);

        if (validate) {
            saveMove(move, currentActivePlayer);
            doMove(move);
        } else {
            System.err.println("Nieprawidłowy ruch.");
        }
    }

    /**
     * Flaga określająca, czy gracz wykonuje skok.
     */
    boolean jump = false;

    /**
     * Współrzędne X i Y początkowe skoku.
     */
    int jumpX, jumpY;

    /**
     * Waliduje ruch w standardowym trybie gry.
     *
     * @param move Ruch w formacie "x1 y1 x2 y2".
     * @return True, jeśli ruch jest poprawny; false w przeciwnym razie.
     */
    public boolean validateStandardMove(String move) {
        if (!move.matches("\\d+ \\d+ \\d+ \\d+"))
            return false;
        String[] parts = move.split(" ");
        int x1 = Integer.parseInt(parts[0]);
        int y1 = Integer.parseInt(parts[1]);
        int x2 = Integer.parseInt(parts[2]);
        int y2 = Integer.parseInt(parts[3]);
        if (jump && !(x1 == jumpX && y1 == jumpY))
            return false;
        if (jump && !((x2 - x1 == 2) || (x1 - x2 == 2) || (y2 - y1 == 2) || (y1 - y2 == 2)))
            return false;
        if (x2 >= 17 || y2 >= 17)
            return false;
        if (board[x1][y1] != currentActivePlayer)
            return false;
        if (board[x2][y2] != 0)
            return false;
        if ((y2 - y1 > 2 || y2 - y1 < -2 || x2 - x1 > 2 || x2 - x1 < -2))
            return false;
        if (x2 == x1) {
            if (!((y1 - y2 == 1) || (y2 - y1 == 1))) {
                if (!((y2 - y1 == 2 && board[x1][y1 + 1] > 0) || (y1 - y2 == 2 && board[x1][y1 - 1] > 0)))
                    return false;
                jump = true;
                jumpX = x2;
                jumpY = y2;
            }
        } else if (x2 - x1 == 2) {
            if (!((y2 - y1 == 2 && board[x1 + 1][y1 + 1] > 0) || (y1 == y2 && board[x1 + 1][y1] > 0)))
                return false;
            jump = true;
            jumpX = x2;
            jumpY = y2;
        } else if (x1 - x2 == 2) {
            if (!((y1 - y2 == 2 && board[x1 - 1][y1 - 1] > 0) || (y1 == y2 && board[x1 - 1][y1] > 0)))
                return false;
            jump = true;
            jumpX = x2;
            jumpY = y2;
        }
        if (x2 > x1 && y2 < y1)
            return false;
        if (x2 < x1 && y2 > y1)
            return false;
        if (board[x1][y1] == check[x1][y1] && check[x1][y1] != check[x2][y2])
            return false;
        return true;
    }

    /**
     * Waliduje ruch w szybkim trybie gry.
     *
     * @param move Ruch w formacie "x1 y1 x2 y2".
     * @return True, jeśli ruch jest poprawny; false w przeciwnym razie.
     */
    public boolean validateSpeedMove(String move) {
        if (!move.matches("\\d+ \\d+ \\d+ \\d+"))
            return false;
        String[] parts = move.split(" ");
        int x1 = Integer.parseInt(parts[0]);
        int y1 = Integer.parseInt(parts[1]);
        int x2 = Integer.parseInt(parts[2]);
        int y2 = Integer.parseInt(parts[3]);
        if (jump && !(x1 == jumpX && y1 == jumpY))
            return false;
        if (jump && !((x2 - x1 >= 2) || (x1 - x2 >= 2) || (y2 - y1 >= 2) || (y1 - y2 >= 2)))
            return false;
        if (board[x1][y1] != currentActivePlayer)
            return false;
        if (board[x2][y2] != 0)
            return false;
        if ((Math.abs(y2 - y1) > 2 && Math.abs(y2 - y1) % 2 == 1) || (Math.abs(x2 - x1) > 2 && Math.abs(x2 - x1) % 2 == 1))
            return false;

        if (x2 == x1 && Math.abs(y2 - y1) >= 2) {
            if (board[x1][(y1 + y2) / 2] <= 0)
                return false;
            int ym;
            if (y1 > y2)
                ym = y2;
            else
                ym = y1;
            for (int i = ym; i < y2 + y1 - ym - 1; i++) {
                if (board[x1][1 + i] != 0 && (1 + i) != ((y1 + y2) / 2))
                    return false;
            }
            jump = true;
            jumpX = x2;
            jumpY = y2;
        } else if (x2 - x1 >= 2) {
            if (y1 == y2) {
                if (board[(x1 + x2) / 2][y1] <= 0)
                    return false;
                for (int i = x1; i < x2 - 1; i++) {
                    if (board[1 + i][y1] != 0 && (1 + i) != ((x1 + x2) / 2))
                        return false;
                }
            } else if (y2 - y1 == x2 - x1) {
                if (board[(x1 + x2) / 2][(y1 + y2) / 2] <= 0)
                    return false;
                for (int i = 1; i < x2 - x1; i++) {
                    if (board[x1 + i][y1 + i] != 0 && (x1 + i) != ((x1 + x2) / 2))
                        return false;
                }
            } else
                return false;
            jump = true;
            jumpX = x2;
            jumpY = y2;
        } else if (x1 - x2 >= 2) {
            if (y1 == y2) {
                if (board[(x1 + x2) / 2][y1] <= 0)
                    return false;
                for (int i = x2; i < x1 - 1; i++) {
                    if (board[1 + i][y1] != 0 && (1 + i) != ((x1 + x2) / 2))
                        return false;
                }
            } else if (y1 - y2 == x1 - x2) {
                if (board[(x1 + x2) / 2][(y1 + y2) / 2] <= 0)
                    return false;
                for (int i = 1; i < x1 - x2; i++) {
                    if (board[x2 + i][y2 + i] != 0 && (x2 + i) != ((x1 + x2) / 2))
                        return false;
                }
            } else
                return false;
            jump = true;
            jumpX = x2;
            jumpY = y2;
        }

        if (x2 > x1 && y2 < y1)
            return false;
        if (x2 < x1 && y2 > y1)
            return false;
        if (board[x1][y1] == check[x1][y1] && check[x1][y1] != check[x2][y2])
            return false;
        return true;
    }

    /**
     * Aktualizuje interfejs użytkownika, aby wskazywał aktualnie aktywnego gracza.
     *
     * @param playerId Identyfikator gracza.
     */
    private void updateActivePlayer(int playerId) {
        currentActivePlayer = playerId;
        activePlayerLabel.setText("Gracz: " + playerId);
        playerColorBox.setFill(getPlayerColor(playerId));
    }

    /**
     * Zwraca kolor przypisany do danego gracza na podstawie jego identyfikatora.
     *
     * @param playerId identyfikator gracza (od 1 do 6).
     * @return kolor odpowiadający graczowi; czarny, jeśli identyfikator nie mieści się w zakresie.
     */
    private Color getPlayerColor(int playerId) {
        switch (playerId) {
            case 1:
                return Color.RED;
            case 2:
                return Color.BLUE;
            case 3:
                return Color.GREEN;
            case 4:
                return Color.YELLOW;
            case 5:
                return Color.PURPLE;
            case 6:
                return Color.ORANGE;
            default:
                return Color.BLACK;
        }
    }

    /**
     * Obsługuje zakończenie tury przez aktualnie aktywnego gracza.
     * Wyświetla komunikat o rezygnacji z ruchu i zmienia aktywnego gracza na następnego.
     */
    public void onPassTurn() {
        System.out.println("Gracz " + currentActivePlayer + " rezygnuje z ruchu.");
        for (Server.Player player : players) {
            player.out.println("Gracz " + currentActivePlayer + " rezygnuje z ruchu.");
        }
        updateToNextActivePlayer();
        jump = false;
    }

    /**
     * Sprawdza, czy aktualny gracz spełnił warunki wygranej.
     * Jeśli tak, przypisuje mu miejsce w klasyfikacji końcowej i zmienia aktywnego gracza.
     * Jeśli wszyscy gracze zakończyli grę, aplikacja zostaje zamknięta.
     *
     * @throws InterruptedException jeśli wystąpi problem z obsługą wątku (np. w czasie oczekiwania na zakończenie gry).
     */
    @FXML
    public void onCheckWinner() throws InterruptedException {
        int count = 0;
        for (int i = 0; i < 17; i++) {
            for (int j = 0; j < 17; j++) {
                if (board[i][j] == check[i][j] && check[i][j] == currentActivePlayer) {
                    count++;
                }
            }
        }
        if (count == 10) {
            System.out.println("Gracz " + currentActivePlayer + " zajął " + place + " miejsce");
            place++;
            if (place == winners.length - 1) {
                System.out.println("Gra się skończyła!");
                Platform.exit();
                Thread.sleep(2000);
            }
            winners[currentActivePlayer]++;
            updateToNextActivePlayer();
        } else {
            System.out.println("Jeszcze nie wygrałeś!");
        }
    }

    /**
     * Aktualizuje identyfikator aktywnego gracza na następnego, pomijając graczy, którzy zakończyli grę.
     */
    private void updateToNextActivePlayer() {
        do {
            currentActivePlayer = (currentActivePlayer % players.size()) + 1;
        } while (winners[currentActivePlayer] == 1);
        updateActivePlayer(currentActivePlayer);
    }

    /**
     * Aktywuje tryb szybkiej gry, ustawiając flagę `szybkie` na true.
     */
    public void szybkie() {
        szybkie = true;
    }

}