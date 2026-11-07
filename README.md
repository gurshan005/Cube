# Cube

An interactive Rubik’s cube built with Java Swing. Drag to inspect the cube from different angles, or enter face turns and watch them animate in sequence.

## Run it

With a JDK installed, run these commands from the repository root:

```sh
mkdir -p out
javac -d out RubicksCube/src/cube/MainCube.java
java -cp out cube.MainCube
```

Select **Perform Move(s)** and try `F, R', U2`: turn the front face, turn the right face in reverse, then turn the upper face twice. The logical rotation buttons change which face a move applies to. Dragging changes only the viewing angle.

[MainCube.java](RubicksCube/src/cube/MainCube.java) holds the sticker state, queues moves, and projects the rotating faces into the Swing panel. The **Solve Cube** button is a placeholder; the current implementation supports manual play.
