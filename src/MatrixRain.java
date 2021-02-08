/******************************************************************************
 MIT License
 Copyright (c) 2021 matanai
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 *******************************************************************************/

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Matrix digital rain generator using Swing library
 *
 * @author matanai
 * @version 1.01
 */
public class MatrixRain extends JPanel
{
    // recommended values
    private static final int SCREEN_WIDTH = 1400; // 1400
    private static final int SCREEN_HEIGHT = 700; // 700
    private static final int FONT_SIZE = 14; // 14
    private static final int DELAY_MILLIS = 30; // 45
    private static final int ARRAY_MAX_LENGTH = 62; // 62

    // this constant sets the ratio between the raindrop's length
    // and color so that the shorter raindrop appears darker
    private static final int LENGTH_COLOR_RATIO =
            (int) (ARRAY_MAX_LENGTH * 0.5d); // 0.5d

    // if set to false each x position will be occupied only by one
    // single raindrop at a time. No other raindrops can emerge at
    // this position until the raindrop reaches bottom of the screen
    private static final boolean RAINDROPS_CAN_OVERLAP = false;

    // fixed pool of raindrops
    private final List<Raindrop> listRaindrops;

    // map of available x positions
    private final Map<Integer, Boolean> mapAvailableXPos = new HashMap<>();

    // max amount of raindrops instantiated. CAUTION: if
    // RAINDROPS_CAN_OVERLAP is set to false, max amount
    // of raindrops can't exceed SCREEN_WIDTH / FONT_SIZE
    private static int maxRaindrops = 200; // 200

    // nested class to represent a stream of symbols falling
    // from the top of the screen (aka "raindrop")
    private static class Raindrop
    {
        private int x;
        private int y;
        private String[] array;
    }

    /**
     * Instantiates the list of raindrops. If RAINDROPS_CAN_OVERLAP is
     * set to false, the constructor will reset max amount of raindrops
     * to match the total number of x positions
     */
    public MatrixRain() {
        listRaindrops = new ArrayList<>();
        // number of raindrops must be equal to available x positions
        if (!RAINDROPS_CAN_OVERLAP) {
            maxRaindrops = SCREEN_WIDTH / FONT_SIZE;
            populateMap();
        }
        populateList();
    }

    /**
     * Populates the map of available x positions. This method is
     * called only if RAINDROPS_CAN_OVERLAP is set to false
     */
    public void populateMap() {
        int value = 0;
        while (value < SCREEN_WIDTH) {
            mapAvailableXPos.put(value, true);
            value += FONT_SIZE;
        }
    }

    /**
     * Add new raindrops to the fixed pool. Each raindrop must have
     * its properties set prior to entering the pool
     */
    private void populateList() {
        for (int i = 0; i < maxRaindrops; i++) {
            listRaindrops.add(prepareRaindrop(new Raindrop()));
        }
    }

    /**
     * Init / update the properties of the raindrop: set y coordinate to zero, generate
     * random x coordinate, generate an array of symbols of arbitrary length. This method
     * is called once when the raindrop pool is being populated at the beginning, and
     * then every time a raindrop reaches the end of the screen.
     *
     * @param raindrop - the raindrop to init / update
     * @return init / updated raindrop
     */
    private Raindrop prepareRaindrop(Raindrop raindrop) {
        // y coordinate must be reset to zero every time the raindrop reaches the end
        // of the screen. Otherwise, it will continue moving past the screen endlessly
        raindrop.y = 0;
        raindrop.x = generateRaindropPosition();
        raindrop.array = generateRaindropArray();
        return raindrop;
    }

    /**
     * If RAINDROPS_CAN_OVERLAP is set to false, this method will keep calling randomizer
     * until it matches any key in the map of available x positions, which is set to true.
     * If the matching x position was found and is true (available), the method will return
     * this x position and set its map counterpart to false (unavailable).
     *
     * If RAINDROPS_CAN_OVERLAP is set to true, this method will return an arbitrary x
     * position, without ever checking the map
     *
     * @return arbitrary x position
     */
    private int generateRaindropPosition() {
        Random randomizer = new Random();

        if (!RAINDROPS_CAN_OVERLAP) {
            int tmp;
            do {
                tmp = randomizer.nextInt(SCREEN_WIDTH / FONT_SIZE) * FONT_SIZE;
            } while (!mapAvailableXPos.get(tmp));

            mapAvailableXPos.put(tmp, false);
            return tmp;
        }
        else {
            return randomizer.nextInt(SCREEN_WIDTH / FONT_SIZE) * FONT_SIZE;
        }
    }

    /**
     * Generates an array of katakana symbols of arbitrary length and order
     *
     * @return new array of symbols
     */
    private String[] generateRaindropArray() {
        Random randomizer = new Random();

        // generate array of symbols in the range from 1 to ARRAY_MAX_LENGTH
        String[] array = new String[randomizer.nextInt(ARRAY_MAX_LENGTH) + 1];

        // randomly populate the array with katakana symbols
        for (int i = 0; i < array.length; i++) {
            char c = (char) (0x30A0 + randomizer.nextInt(96));
            array[i] = Character.toString(c);
        }

        return array;
    }

    /**
     * Execute drawing routines
     *
     * @param g - the graphics context in which to paint
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font("Monospaced", Font.BOLD, FONT_SIZE));

        // calculate the raindrops pool state to display
        drawRaindrops(g2d);

        // slow down the frame
        try {
            Thread.sleep(DELAY_MILLIS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        repaint();
    }

    /**
     * Calculate and draw a frame with each raindrop state
     *
     * @param g2d - the graphics context in which to paint
     */
    private void drawRaindrops(Graphics2D g2d) {
        for (Raindrop raindrop : listRaindrops) {
            // update raindrop y coordinate
            raindrop.y += FONT_SIZE;

            // iterate through each symbol in the raindrop array
            for (int i = 0; i < raindrop.array.length; i++) {

                // get color for a symbol
                g2d.setColor(getColor(i, raindrop));

                // calculate the offset for the frozen symbols effect and draw frame
                int offset = Math.abs((i - raindrop.y / FONT_SIZE) % raindrop.array.length);
                g2d.drawString(raindrop.array[offset], raindrop.x, raindrop.y - (i * FONT_SIZE));
            }

            // when a raindrop reaches the bottom of the screen, move it to the top again
            if (raindrop.y - (raindrop.array.length * FONT_SIZE) >= SCREEN_HEIGHT) {
                // update this x position as available for other raindrops
                if (!RAINDROPS_CAN_OVERLAP) {
                    mapAvailableXPos.replace(raindrop.x, true);
                }
                prepareRaindrop(raindrop);
            }
        }
    }

    /**
     * Pick the color for a raindrop symbol at a given index
     *
     * @param index - an array index, containing the symbol
     * @param raindrop - raindrop, which is being processed
     * @return new instance of class Color
     */
    private Color getColor(int index, Raindrop raindrop) {
        assert raindrop != null;

        // calculate color intensity so that the raindrop fades out while
        // moving using simple linear conversion of one range to another
        int max = Math.abs((index - raindrop.array.length) * 255 / raindrop.array.length);
        int min = Math.abs((index - raindrop.array.length) * 100 / raindrop.array.length);

        Color color;

        // shorter raindrops must appear darker
        color = (raindrop.array.length < LENGTH_COLOR_RATIO)
                ? new Color(0, 50, 0, max)
                : new Color(min, max, min, max);

        // first symbol must be the brightest
        if (index == 0) {
            color = (raindrop.array.length < LENGTH_COLOR_RATIO)
                ? new Color(25, 50, 25)
                : new Color(200, 255, 200);
        }

        return color;
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Matrix digital rain - by matanai");
        frame.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new MatrixRain());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}