package com.example.websocketTest;

class CoordinatePair {
    /**
     * x coordinate of the pair.
     */
    private float x;
    /**
     * y coordinate of the pair.
     */
    private float y;

    /**
     * Create a coordinate pair using x and y value.
     *
     * @param xVal the x coordinate of the pair.
     * @param yVal the y coordinate of the pair.
     */
    CoordinatePair(final float xVal, final float yVal) {
        this.x = xVal;
        this.y = yVal;
    }

    /**
     * Check if two coordinate pairs are the same.
     *
     * @param second the coordinate pair to be examined
     * @return whether the two coordinate pairs are the same
     */
    boolean equals(final CoordinatePair second) {
        return (x == second.getX() && y == second.getY());
    }

    /**
     * Get the x coordinate of the pair.
     *
     * @return x coordinate of the pair
     */
    float getX() {
        return x;
    }

    /**
     * Get the y coordinate of the pair.
     *
     * @return y coordinate of the pair
     */
    float getY() {
        return y;
    }

    byte getDirection(CoordinatePair pair) {
        if (Math.abs(pair.getY() - y) > Math.abs(pair.getX() - x)) {
            if (pair.getY() > y) {
                return Controls.MOVE_UP;
            } else {
                return Controls.MOVE_DOWN;
            }
        } else {
            if (pair.getX() > x) {
                return Controls.MOVE_LEFT;
            } else {
                return Controls.MOVE_RIGHT;
            }
        }
    }

    double getPreciseDirection(CoordinatePair pair) {
        float deltaX = pair.getX() - x;
        float deltaY = y - pair.getY();
        double angle = Math.atan2(deltaX, deltaY);
        return (angle > 0) ? angle : angle + Math.PI * 2;
    }

    double getDistance(CoordinatePair pair) {
        float deltaX = pair.getX() - x;
        float deltaY = pair.getY() - y;
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }

    @Override
    public String toString() {
        return ((int) x) + " " + ((int) y);
    }

    float getXChange(CoordinatePair pair) {
        return x - pair.getX();
    }

    float getYChange(CoordinatePair pair) {
        return y - pair.getY();
    }

}
