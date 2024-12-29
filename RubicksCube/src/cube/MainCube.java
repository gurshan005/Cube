package cube;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class MainCube {
	

    private JFrame frame;
    private RotatingCubePanel cubePanel;

    public MainCube() {
        initializeGUI();
    }

    private void initializeGUI() {
        frame = new JFrame("Rubik's Cube (Orientation + Partial Rotation) - Physically Clockwise");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 800);
        frame.setLayout(new BorderLayout());

        JLabel title = new JLabel(
            "Cube: L/R Buttons for Orientation; Moves are Physically Clockwise from the Face's Perspective",
            JLabel.CENTER
        );
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        frame.add(title, BorderLayout.NORTH);

        cubePanel = new RotatingCubePanel();
        frame.add(cubePanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        JButton solveButton = new JButton("Solve Cube");
        solveButton.setFont(new Font("Arial", Font.BOLD, 16));
        solveButton.setBackground(Color.CYAN);
        solveButton.setFocusPainted(false);
        solveButton.addActionListener(e -> solveCube());
        buttonPanel.add(solveButton);

        JButton moveButton = new JButton("Perform Move(s)");
        moveButton.setFont(new Font("Arial", Font.BOLD, 16));
        moveButton.setBackground(Color.GREEN);
        moveButton.setFocusPainted(false);
        moveButton.addActionListener(e -> performMoves());
        buttonPanel.add(moveButton);

        // Rotate orientation LEFT
        JButton rotateLeftButton = new JButton("Rotate Right (Logical)");
        rotateLeftButton.setFont(new Font("Arial", Font.BOLD, 16));
        rotateLeftButton.setFocusPainted(false);
        rotateLeftButton.addActionListener(e -> cubePanel.rotateOrientationLeft());
        buttonPanel.add(rotateLeftButton);

        // Rotate orientation RIGHT
        JButton rotateRightButton = new JButton("Rotate Left (Logical)");
        rotateRightButton.setFont(new Font("Arial", Font.BOLD, 16));
        rotateRightButton.setFocusPainted(false);
        rotateRightButton.addActionListener(e -> cubePanel.rotateOrientationRight());
        buttonPanel.add(rotateRightButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void solveCube() {
        // Placeholder
        JOptionPane.showMessageDialog(
            frame,
            "Solve logic not implemented.",
            "Solve Cube",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void performMoves() {
        String input = JOptionPane.showInputDialog(
            frame,
            "Enter moves (e.g. F, R', U2). If a face is pointing toward you, we flip the angle so it's physically clockwise."
        );
        if (input != null && !input.trim().isEmpty()) {
            cubePanel.queueMoves(input.trim());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainCube::new);
    }

    //=========================================================
    //==================  ROTATING CUBE PANEL  ================
    //=========================================================
    private class RotatingCubePanel extends JPanel {

        /**
         * 6 faces in data:
         *   0 = White(Up),
         *   1 = Red(Front),
         *   2 = Green(Right),
         *   3 = Yellow(Down),
         *   4 = Orange(Left),
         *   5 = Blue(Back).
         *
         * orientation[0..5] => front, right, back, left, up, down
         */
        private int[] orientation = {1, 2, 5, 4, 0, 3};

        // The 6 faces × 3×3 color state
        Color[][][] cubeState = new Color[6][3][3];

        // For partial rotation
        private boolean animatingMove = false;
        private Deque<String> moveQueue = new ArrayDeque<>();
        private String currentMove;
        private double animatedAngle = 0; 
        private Timer animationTimer;
        private int rotatingFaceIndex = -1; // physically which face is turning?

        // For rotating the entire view with mouse
        private double angleX = 0.0;
        private double angleY = 0.0;
        private int prevMouseX, prevMouseY;

        // We'll define a simple camera direction = (0,0,-1) if the user looks along -Z.
        private final double[] cameraDir = {0, 0, -1};

        // Default outward normals for each face (0..5):
        //   0=Up ( y=+1 ), 1=Front ( z=-1 ), 2=Right ( x=+1 ),
        //   3=Down( y=-1 ), 4=Left( x=-1 ), 5=Back( z=+1 ).
        private final double[][] faceNormalsDefault = {
            {0, +1,  0},  // 0=Up
            {0,  0, -1},  // 1=Front
            {+1, 0,  0},  // 2=Right
            {0, -1,  0},  // 3=Down
            {-1, 0,  0},  // 4=Left
            {0,  0, +1}   // 5=Back
        };

        public RotatingCubePanel() {
            setBackground(Color.DARK_GRAY);
            initCubeState();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    prevMouseX = e.getX();
                    prevMouseY = e.getY();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (animatingMove) return;
                    int dx = e.getX() - prevMouseX;
                    int dy = e.getY() - prevMouseY;
                    angleY += dx * 0.01;
                    angleX += dy * 0.01;
                    prevMouseX = e.getX();
                    prevMouseY = e.getY();
                    repaint();
                }
            });
        }

        private void initCubeState() {
            // 0=White,1=Red,2=Green,3=Yellow,4=Orange,5=Blue
            for (int r=0; r<3; r++) {
                for (int c=0; c<3; c++) {
                    cubeState[0][r][c] = Color.WHITE;
                    cubeState[1][r][c] = Color.RED;
                    cubeState[2][r][c] = Color.GREEN;
                    cubeState[3][r][c] = Color.YELLOW;
                    cubeState[4][r][c] = Color.ORANGE;
                    cubeState[5][r][c] = Color.BLUE;
                }
            }
        }

        // orientation: front->right, right->back, back->left, left->front
        public void rotateOrientationLeft() {
            int oldFront = orientation[0];
            int oldRight = orientation[1];
            int oldBack  = orientation[2];
            int oldLeft  = orientation[3];

            orientation[0] = oldRight; 
            orientation[1] = oldBack;  
            orientation[2] = oldLeft;  
            orientation[3] = oldFront; 
        }

        // orientation: front->left, left->back, back->right, right->front
        public void rotateOrientationRight() {
            int oldFront = orientation[0];
            int oldRight = orientation[1];
            int oldBack  = orientation[2];
            int oldLeft  = orientation[3];

            orientation[0] = oldLeft;  
            orientation[1] = oldFront; 
            orientation[2] = oldRight; 
            orientation[3] = oldBack;  
        }

        public void queueMoves(String moves) {
            String[] arr = moves.split(",");
            for (String m : arr) {
                String mv = m.trim().toUpperCase();
                if (!mv.isEmpty()) moveQueue.add(mv);
            }
            if (!animatingMove && !moveQueue.isEmpty()) {
                startNextMove();
            }
        }

        private void startNextMove() {
            if (moveQueue.isEmpty()) return;
            String mv = moveQueue.poll();
            startMoveAnimation(mv);
        }

        /**
         * If the face's outward normal is pointing toward the camera (dot>0),
         * we invert the angle, so the move is physically clockwise from that face's perspective.
         */
        public void startMoveAnimation(String move) {
            if (animatingMove) return;
            currentMove = move;
            animatingMove = true;
            animatedAngle = 0;

            // 1) Decide the base angle => ±(π/2) or ±(π)
            double baseAngle = Math.PI / 2; 
            char suffix = (move.length()>1) ? move.charAt(1) : ' ';
            if (suffix == '2') {
                baseAngle = Math.PI; 
            } else if (suffix == '\'') {
                baseAngle = -baseAngle;
            }

            // 2) Identify which face is physically rotating
            char letter = move.charAt(0);
            int faceIndex = -1;
            switch(letter) {
                case 'F': faceIndex = orientation[0]; break;
                case 'R': faceIndex = orientation[1]; break;
                case 'B': faceIndex = orientation[2]; break;
                case 'L': faceIndex = orientation[3]; break;
                case 'U': faceIndex = orientation[4]; break;
                case 'D': faceIndex = orientation[5]; break;
                default:
                    JOptionPane.showMessageDialog(this, "Unknown move: " + move);
                    animatingMove = false;
                    return;
            }
            rotatingFaceIndex = faceIndex;

            // 3) Flip sign if face is pointing TOWARD the camera
            double[] faceNorm = faceNormalsDefault[faceIndex];
            // rotate that normal by angleX, angleY
            double[] rotatedNorm = rotateXYZ(faceNorm, angleX, angleY, 0);
            double d = dot(rotatedNorm, cameraDir);
            // if dot>0 => face is pointing toward camera => invert
            if (d > 0) {
                baseAngle = -baseAngle;
            }

            final double finalBaseAngle = baseAngle;
            final String finalMove = currentMove;

            // 4) Animate in 10 steps
            animationTimer = new Timer(20, new ActionListener() {
                int steps = 10;
                double stepAngle = finalBaseAngle / steps;

                @Override
                public void actionPerformed(ActionEvent e) {
                    animatedAngle += stepAngle;
                    if ((finalBaseAngle >= 0 && animatedAngle >= finalBaseAngle) ||
                        (finalBaseAngle < 0 && animatedAngle <= finalBaseAngle))
                    {
                        animatedAngle = finalBaseAngle;
                        animationTimer.stop();
                        finishMove(finalMove);
                    }
                    repaint();
                }
            });
            animationTimer.start();
        }

        private void finishMove(String move) {
            applyMoveToState(move);

            animatingMove = false;
            rotatingFaceIndex = -1;
            currentMove = null;
            animatedAngle = 0;
            repaint();

            if (!moveQueue.isEmpty()) {
                startNextMove();
            }
        }

        private void applyMoveToState(String move) {
            char letter = move.charAt(0);
            char suffix = (move.length()>1) ? move.charAt(1) : ' ';
            int timesCW = 1;
            if (suffix=='2') timesCW=2;
            else if (suffix=='\'') timesCW=3;

            int faceIndex;
            switch(letter) {
                case 'F': faceIndex = orientation[0]; break;
                case 'R': faceIndex = orientation[1]; break;
                case 'B': faceIndex = orientation[2]; break;
                case 'L': faceIndex = orientation[3]; break;
                case 'U': faceIndex = orientation[4]; break;
                case 'D': faceIndex = orientation[5]; break;
                default:
                    JOptionPane.showMessageDialog(this, "Unknown move: " + move);
                    return;
            }

            for (int i=0; i<timesCW; i++) {
                rotateFaceCW(faceIndex);
                cycleEdges(faceIndex);
            }
        }

        private void rotateFaceCW(int face) {
            Color[][] temp = new Color[3][3];
            for (int r=0; r<3; r++) {
                for (int c=0; c<3; c++) {
                    temp[c][2-r] = cubeState[face][r][c];
                }
            }
            cubeState[face] = temp;
        }

        private void cycleEdges(int face) {
            switch(face) {
                case 0: cycleEdgesU(); break; 
                case 1: cycleEdgesF(); break; 
                case 2: cycleEdgesR(); break; 
                case 3: cycleEdgesD(); break; 
                case 4: cycleEdgesL(); break; 
                case 5: cycleEdgesB(); break;
            }
        }

        // Edge cycles
        private Color[] getRow(int face, int row) {
            return cubeState[face][row].clone();
        }
        private void setRow(int face, int row, Color[] vals) {
            for (int i=0; i<3; i++) {
                cubeState[face][row][i] = vals[i];
            }
        }
        private Color[] getCol(int face, int col) {
            Color[] arr = new Color[3];
            for (int i=0; i<3; i++) {
                arr[i] = cubeState[face][i][col];
            }
            return arr;
        }
        private void setCol(int face, int col, Color[] vals) {
            for (int i=0; i<3; i++) {
                cubeState[face][i][col] = vals[i];
            }
        }
        private Color[] reverse(Color[] arr) {
            Color[] copy = arr.clone();
            for (int i=0; i<copy.length/2; i++) {
                Color tmp = copy[i];
                copy[i] = copy[copy.length-1-i];
                copy[copy.length-1-i] = tmp;
            }
            return copy;
        }

        private void cycleEdgesF() {
            Color[] Urow = getRow(0,2);
            Color[] Lcol = getCol(4,2);
            Color[] Drow = getRow(3,0);
            Color[] Rcol = getCol(2,0);

            setRow(0,2, reverse(Rcol));
            setCol(4,2, Urow);
            setRow(3,0, reverse(Lcol));
            setCol(2,0, Drow);
        }
        private void cycleEdgesR() {
            Color[] Ucol = getCol(0,2);
            Color[] Fcol = getCol(1,2);
            Color[] Dcol = getCol(3,2);
            Color[] Bcol = getCol(5,0);

            setCol(0,2, reverse(Bcol));
            setCol(1,2, Ucol);
            setCol(3,2, Fcol);
            setCol(5,0, reverse(Dcol));
        }
        private void cycleEdgesL() {
            Color[] Ucol = getCol(0,0);
            Color[] Bcol = getCol(5,2);
            Color[] Dcol = getCol(3,0);
            Color[] Fcol = getCol(1,0);

            setCol(0,0, Fcol);
            setCol(5,2, reverse(Ucol));
            setCol(3,0, reverse(Bcol));
            setCol(1,0, Dcol);
        }
        private void cycleEdgesU() {
            Color[] Frow = getRow(1,0);
            Color[] Rrow = getRow(2,0);
            Color[] Brow = getRow(5,0);
            Color[] Lrow = getRow(4,0);

            setRow(1,0, Lrow);
            setRow(2,0, Frow);
            setRow(5,0, Rrow);
            setRow(4,0, Brow);
        }
        private void cycleEdgesD() {
            Color[] Frow = getRow(1,2);
            Color[] Lrow = getRow(4,2);
            Color[] Brow = getRow(5,2);
            Color[] Rrow = getRow(2,2);

            setRow(1,2, Rrow);
            setRow(4,2, Frow);
            setRow(5,2, Lrow);
            setRow(2,2, Brow);
        }
        private void cycleEdgesB() {
            Color[] Urow = getRow(0,0);
            Color[] Rcol = getCol(2,2);
            Color[] Drow = getRow(3,2);
            Color[] Lcol = getCol(4,0);

            setRow(0,0, reverse(Rcol));
            setCol(2,2, reverse(Drow));
            setRow(3,2, Lcol);
            setCol(4,0, Urow);
        }

        // ------------------ 3D RENDERING -------------------
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D)g;
            int w = getWidth();
            int h = getHeight();

            List<RenderTile> tileList = new ArrayList<>();
            for (int f=0; f<6; f++) {
                addFaceTiles(tileList, f);
            }
            tileList.sort((a,b)->Double.compare(a.avgZ, b.avgZ));

            for (RenderTile rt : tileList) {
                int[] xPoints = new int[4];
                int[] yPoints = new int[4];
                for (int i=0; i<4; i++) {
                    double[] proj = project(rt.rotatedCorners[i]);
                    xPoints[i] = (int)(proj[0] + w/2);
                    yPoints[i] = (int)(proj[1] + h/2);
                }
                g2d.setColor(rt.color);
                g2d.fillPolygon(xPoints, yPoints, 4);
                g2d.setColor(Color.BLACK);
                g2d.drawPolygon(xPoints, yPoints, 4);
            }
        }

        private class RenderTile {
            double[][] rotatedCorners;
            Color color;
            double avgZ;
            RenderTile(double[][] rc, Color c, double z) {
                rotatedCorners = rc;
                color = c;
                avgZ = z;
            }
        }

        private void addFaceTiles(List<RenderTile> list, int face) {
            double[] div = {-1, -1.0/3.0, 1.0/3.0, 1};
            for (int r=0; r<3; r++) {
                for (int c=0; c<3; c++) {
                    Color col = cubeState[face][r][c];
                    double[][] corners = faceTileCorners(face, r, c, div);

                    double[][] partialRot = corners;
                    if (animatingMove && face == rotatingFaceIndex) {
                        partialRot = applyPartialRotationToTile(corners, face, animatedAngle);
                    }

                    double[][] finalPos = new double[4][3];
                    double avgZ=0;
                    for (int i=0; i<4; i++) {
                        finalPos[i] = rotateXYZ(partialRot[i], angleX, angleY, 0);
                        avgZ += finalPos[i][2];
                    }
                    avgZ/=4.0;

                    list.add(new RenderTile(finalPos, col, avgZ));
                }
            }
        }

        private double[][] applyPartialRotationToTile(double[][] corners, int face, double angle) {
            double[][] newPos = new double[4][3];
            double cosA = Math.cos(angle), sinA = Math.sin(angle);

            int axis=2;
            double shiftX=0, shiftY=0, shiftZ=0;
            switch(face) {
                case 0: // Up => pivot around y=+1 => axis=1
                    axis=1; shiftY=-1;
                    break;
                case 1: // Front => pivot around z=-1 => axis=2
                    axis=2; shiftZ=+1; 
                    break;
                case 2: // Right => pivot around x=+1 => axis=0
                    axis=0; shiftX=-1;
                    break;
                case 3: // Down => pivot around y=-1 => axis=1
                    axis=1; shiftY=+1;
                    break;
                case 4: // Left => pivot around x=-1 => axis=0
                    axis=0; shiftX=+1;
                    break;
                case 5: // Back => pivot around z=+1 => axis=2
                    axis=2; shiftZ=-1;
                    break;
            }

            for (int i=0; i<4; i++) {
                double X = corners[i][0]+ shiftX;
                double Y = corners[i][1]+ shiftY;
                double Z = corners[i][2]+ shiftZ;

                double Xr=X, Yr=Y, Zr=Z;
                if (axis==0) {
                    Yr = Y*cosA - Z*sinA;
                    Zr = Y*sinA + Z*cosA;
                } else if (axis==1) {
                    Xr = X*cosA + Z*sinA;
                    Zr = -X*sinA + Z*cosA;
                } else {
                    Xr = X*cosA - Y*sinA;
                    Yr = X*sinA + Y*cosA;
                }

                Xr-= shiftX;
                Yr-= shiftY;
                Zr-= shiftZ;

                newPos[i][0]=Xr;
                newPos[i][1]=Yr;
                newPos[i][2]=Zr;
            }
            return newPos;
        }

        /**
         * Face corners in [-1,+1] space
         */
        private double[][] faceTileCorners(int face, int r, int c, double[] div) {
            double X1=div[c], X2=div[c+1];
            double Y1=div[r], Y2=div[r+1];

            double x1,y1,z1,x2_,y2_,z2_,x3,y3,z3,x4,y4,z4;
            switch(face) {
                case 0: // Up => y=+1
                    x1= X1; y1=+1; z1= -Y1;
                    x2_=X1; y2_=+1; z2_=-Y2;
                    x3= X2; y3=+1; z3=-Y2;
                    x4= X2; y4=+1; z4=-Y1;
                    break;
                case 1: // Front => z=-1
                    x1= X1; y1= Y1; z1=-1;
                    x2_=X1; y2_=Y2; z2_=-1;
                    x3= X2; y3=Y2;  z3=-1;
                    x4= X2; y4=Y1;  z4=-1;
                    break;
                case 2: // Right => x=+1
                    x1= +1; y1= Y1; z1= X1;
                    x2_=+1; y2_=Y2; z2_= X1;
                    x3= +1; y3=Y2;  z3= X2;
                    x4= +1; y4=Y1;  z4= X2;
                    break;
                case 3: // Down => y=-1
                    x1= X1; y1=-1; z1= Y1;
                    x2_=X1; y2_=-1; z2_=Y2;
                    x3= X2; y3=-1;  z3=Y2;
                    x4= X2; y4=-1;  z4=Y1;
                    break;
                case 4: // Left => x=-1
                    x1= -1; y1= Y1; z1= -X1;
                    x2_= -1; y2_=Y2; z2_= -X1;
                    x3= -1; y3=Y2;  z3= -X2;
                    x4= -1; y4=Y1;  z4= -X2;
                    break;
                case 5: // Back => z=+1
                    x1= X2; y1= Y1; z1=+1;
                    x2_=X2; y2_=Y2; z2_=+1;
                    x3= X1; y3=Y2;  z3=+1;
                    x4= X1; y4=Y1;  z4=+1;
                    break;
                default:
                    x1=y1=z1=x2_=y2_=z2_=x3=y3=z3=x4=y4=z4=0;
            }
            return new double[][] {
                {x1,y1,z1},
                {x2_,y2_,z2_},
                {x3,y3,z3},
                {x4,y4,z4}
            };
        }

        private double[] rotateXYZ(double[] v, double ax, double ay, double az) {
            double cosX=Math.cos(ax), sinX=Math.sin(ax);
            double y = v[1]*cosX - v[2]*sinX;
            double z = v[1]*sinX + v[2]*cosX;
            double x = v[0];

            double cosY=Math.cos(ay), sinY=Math.sin(ay);
            double x2 = x*cosY + z*sinY;
            double z2 = -x*sinY + z*cosY;

            double cosZ=Math.cos(az), sinZ=Math.sin(az);
            double x3 = x2*cosZ - y*sinZ;
            double y3 = x2*sinZ + y*cosZ;

            return new double[]{x3,y3,z2};
        }

        private double[] project(double[] v) {
            double dist=3.0;
            double factor = 250/(dist - v[2]);
            double px=v[0]*factor;
            double py=-v[1]*factor;
            return new double[]{px,py};
        }

        // Dot product helper
        private double dot(double[] a, double[] b) {
            return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
        }
    }
}