import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.table.DefaultTableModel;



class MainMenu extends JFrame implements ActionListener {
    private final JButton playButton;
    private final JButton highScoresButton;
    private BufferedImage backgroundImage;
    private int selectedDifficulty = 5;  // Default difficulty (you can set your default value)

    public MainMenu() {
        setTitle("Flappy Bird - Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 574);

        try {
            backgroundImage = ImageIO.read(new File("menu.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        playButton = new JButton("Play");
        highScoresButton = new JButton("High Scores");

        playButton.setFocusable(false);
        highScoresButton.setFocusable(false);

        playButton.addActionListener(this);
        highScoresButton.addActionListener(this);

        JPanel innerPanel = new JPanel(new GridBagLayout());
        innerPanel.setPreferredSize(new Dimension(510, 600));
        innerPanel.add(playButton, new GridBagConstraints());
        innerPanel.add(highScoresButton, new GridBagConstraints());
        innerPanel.setOpaque(false);

        JPanel customPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };

        customPanel.add(innerPanel);
        add(customPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == playButton) {
            String username = JOptionPane.showInputDialog(MainMenu.this, "Enter your username:");
            if (username != null && !username.isEmpty()) {
                selectDifficulty();  // Ask the user to select the difficulty
                FlappyBirdGame game = new FlappyBirdGame(username, selectedDifficulty);
                game.setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(MainMenu.this, "Invalid username. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getSource() == highScoresButton) {
            String highScores = getHighScoresFromDatabase();
            new HighScoresDialog(highScores);
        }
    }

    private void selectDifficulty() {
        // Use a String array for difficulty choices
        String[] options = {"Easy", "Medium", "Hard"};
        String selectedOption = (String) JOptionPane.showInputDialog(
                this,
                "Select Difficulty",
                "Difficulty",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                "Medium"
        );

        if (selectedOption != null) {
            // Assign the selected difficulty (pipe speed)
            switch (selectedOption) {
                case "Easy":
                    selectedDifficulty = 5;  // Set your value for Easy
                    break;
                case "Medium":
                    selectedDifficulty = 10;  // Set your value for Medium
                    break;
                case "Hard":
                    selectedDifficulty = 15;// Set your value for Hard
                    break;
            }
        }
    }

    private String getHighScoresFromDatabase() {
        StringBuilder highScores = new StringBuilder();
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/flappy_bird", "root", "mominfaraz");
            Statement statement = connection.createStatement();

            String query = "SELECT * FROM score ORDER BY highscore DESC";
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                String username = resultSet.getString("username");
                int highscore = resultSet.getInt("highscore");

                highScores.append(username).append(" ").append(highscore).append("\n");
            }

            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            highScores.append("Error retrieving high scores!");
        }

        return highScores.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainMenu mainMenu = new MainMenu();
        });
    }
}

class HighScoresDialog extends JDialog {
    private JTable table;

    public HighScoresDialog(String highScores) {
        setTitle("High Scores");
        setSize(400, 300);

        String[] columnNames = {"Username", "High Score"};
        String[] rows = highScores.split("\n");

        String[][] data = new String[rows.length][2];
        for (int i = 0; i < rows.length; i++) {
            String[] cols = rows[i].split(" ");
            data[i][0] = cols[0];
            data[i][1] = cols[1];
        }

        // Use DefaultTableModel to set the table as non-editable
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(300, 200));
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        setLocationRelativeTo(null);
        setVisible(true);
    }
}


class FlappyBirdGame extends JFrame {
    public static final int WIDTH = 1000;
    public static final int HEIGHT = 600;
    private FlappyBirdPanel flappyBirdPanel;
    private String username;
    private int difficulty;

    public FlappyBirdGame(String username, int difficulty) {
        this.difficulty = difficulty;
        this.username = username;

        setTitle("Flappy Bird");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);

        flappyBirdPanel = new FlappyBirdPanel(username, difficulty);
        add(flappyBirdPanel);

        Timer timer = new Timer(1, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                flappyBirdPanel.update();
                flappyBirdPanel.repaint();
            }
        });
        timer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    flappyBirdPanel.jump();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (flappyBirdPanel.isGameOver() && flappyBirdPanel.isRestart()) {
                    flappyBirdPanel.restartGame();
                }
            }
        });

        setFocusable(true);
        requestFocus();
    }
}

class FlappyBirdPanel extends JPanel {
    private int birdY;
    private int birdSpeed;
    private Timer pipeTimer;
    private boolean isJumping;

    private List<Pipe> pipes;
    private Random random;
    private BufferedImage backgroundImage;
    private BufferedImage birdImage;
    private BufferedImage gameOverImage;

    private int score;
    private int highScore;
    private boolean gameOver;
    private boolean restart;
    private String username;
    private int pipeSpeed;
    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isRestart() {
        return restart;
    }
    public void setPipeSpeed(int speed) {
        this.pipeSpeed = speed;
    }

    public FlappyBirdPanel(String username, int selectedDifficulty) {
        this.username = username;
        birdY = 300;
        birdSpeed = 0;
        isJumping = false;

        pipes = new ArrayList<>();
        random = new Random();

        try {
            backgroundImage = ImageIO.read(new File("background.jpeg"));
            birdImage = ImageIO.read(new File("newBird.png"));
            gameOverImage = ImageIO.read(new File("gameover.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        pipeTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addPipe();
            }
        });
        pipeTimer.start();

        setPipeSpeed(selectedDifficulty);

        score = 0;
        highScore = 0;
        gameOver = false;
        restart = false;
    }

    private void playSound(String filename) {
        new Thread(() -> {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filename).getAbsoluteFile());
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void jump() {
        birdSpeed = -10;
        playSound("flap.wav");
    }

    public void update() {
        if (!gameOver) {
            // Move the bird vertically based on its current speed
            birdY = birdY + birdSpeed;

            // Increase the bird's speed due to gravity
            birdSpeed += 1;

            // Ensure the bird stays within the bottom vertical boundary
            if (birdY >= FlappyBirdGame.HEIGHT - 50) {
                birdY = FlappyBirdGame.HEIGHT - 50;  // Set the bird to the bottom of the screen
                birdSpeed = 0;  // Reset the bird's speed to zero
                isJumping = false;  // The bird is not jumping anymore
            }

            // Ensure the bird stays within the top vertical boundary
            if (birdY < 0) {
                birdY = 0;  // Set the bird to the top of the screen
                birdSpeed = 1;  // Reset the bird's speed to simulate bouncing off the top
            }

            // Move and update the pipes
            movePipes();

            // Check for collisions between the bird and the pipes
            checkCollisions();
        }
    }


    private void movePipes() {
        for (Pipe pipe : pipes) {
            pipe.x -= pipeSpeed;
        }
        pipes.removeIf(pipe -> pipe.x + pipe.width < 0);

        if (!pipes.isEmpty() && pipes.get(0).x + pipes.get(0).width < 50) {
            pipes.remove(0);
            pipes.remove(0);
            score++;
            if (score > highScore) {
                highScore = score;
            }
        }
    }

    private void checkCollisions() {
        Rectangle birdRectangle = new Rectangle(50, birdY, 30, 30);

        for (Pipe pipe : pipes) {
            if (birdRectangle.intersects(pipe)) {
                playSound("hit.wav");
                playSound("gameover.wav");

                gameOver = true;
                restart = true;
                storeScoreInDatabase(username, score);
            }
        }
    }

    private void addPipe() {
        int gapHeight = 200;
        int pipeHeight = random.nextInt(FlappyBirdGame.HEIGHT - gapHeight - 50);
        Pipe upperPipe = new Pipe(FlappyBirdGame.WIDTH, 0, 50, pipeHeight, true);
        Pipe lowerPipe = new Pipe(FlappyBirdGame.WIDTH, pipeHeight + gapHeight, 50, FlappyBirdGame.HEIGHT - pipeHeight - gapHeight, false);
        pipes.add(upperPipe);
        pipes.add(lowerPipe);
    }

    private void storeScoreInDatabase(String username, int score) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/flappy_bird", "root", "mominfaraz");
            Statement statement = connection.createStatement();

            String insertQuery = "INSERT INTO score (username, highscore) VALUES ('" + username + "', " + score + ")";
            statement.executeUpdate(insertQuery);

            statement.close();
            connection.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Error storing score in the database!");
        }
    }

    public void restartGame() {
        pipes.clear();
        birdY = 300;
        birdSpeed = 0;
        score = 0;
        gameOver = false;
        restart = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, FlappyBirdGame.WIDTH, FlappyBirdGame.HEIGHT, this);
        }

        if (birdImage != null) {
            int birdSize = 50;
            g.drawImage(birdImage, 50, birdY, birdSize, birdSize, this);
        }

        for (Pipe pipe : pipes) {
            g.drawImage(pipe.getImage(), pipe.x, pipe.y, pipe.width, pipe.height, this);
        }

        if (gameOver) {
            if (gameOverImage != null) {
                int gameOverWidth = gameOverImage.getWidth();
                int gameOverHeight = gameOverImage.getHeight();
                g.drawImage(gameOverImage, (FlappyBirdGame.WIDTH - gameOverWidth) / 2, (FlappyBirdGame.HEIGHT - gameOverHeight) / 2, this);
            }

            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            String highScoreText = "High Score: " + highScore;
            g.drawString(highScoreText, (FlappyBirdGame.WIDTH - g.getFontMetrics().stringWidth(highScoreText)) / 2, FlappyBirdGame.HEIGHT / 2 + 90);

            g.setColor(Color.RED);
            String restartText = "Click to Restart";
            g.drawString(restartText, (FlappyBirdGame.WIDTH - g.getFontMetrics().stringWidth(restartText)) / 2, FlappyBirdGame.HEIGHT / 2 + 120);
        } else {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            String scoreText = "Score: " + score;
            g.drawString(scoreText, 20, 30);
        }
    }
}


class Pipe extends Rectangle {
    private boolean isUp;
    private BufferedImage image;

    public BufferedImage getImage() {
        return image;
    }

    public Pipe(int x, int y, int width, int height, boolean isUp) {
        super(x, y, width, height);
        this.isUp = isUp;

        try {
            if (isUp) {
                image = ImageIO.read(new File("pipe_down.png"));
            } else {
                image = ImageIO.read(new File("pipe_up.png"));
            }

            int imageHeight = isUp ? height : height * 2;
            image = resizeImage(image, width, imageHeight);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("Width and height must be greater than zero");
        }

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resizedImage;
    }
}
