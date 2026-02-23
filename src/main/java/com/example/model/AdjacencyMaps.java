package com.example.model;

import java.util.ArrayList;

public class AdjacencyMaps {
    // { Top, URight, DRight, Down, DLeft, ULeft }
    // This is Joshua's changed version to work with UI Code, old one in numerical
    // order is found below
    public static final int[][] TileVertices = {
        { 1, 2, 10, 9, 8, 0 }, // tile 0
        { 3, 4, 12, 11, 10, 2 }, // tile 1
        { 5, 6, 14, 13, 12, 4 }, // tile 2
        { 8, 9, 19, 18, 17, 7 }, // tile 3
        { 10, 11, 21, 20, 19, 9 }, // tile 4
        { 12, 13, 23, 22, 21, 11 }, // tile 5
        { 14, 15, 25, 24, 23, 13 }, // tile 6
        { 17, 18, 29, 28, 27, 16 }, // tile 7
        { 19, 20, 31, 30, 29, 18 }, // tile 8
        { 21, 22, 33, 32, 31, 20 }, // tile 9
        { 23, 24, 35, 34, 33, 22 }, // tile 10
        { 25, 26, 37, 36, 35, 24 }, // tile 11
        { 29, 30, 40, 39, 38, 28 }, // tile 12
        { 31, 32, 42, 41, 40, 30 }, // tile 13
        { 33, 34, 44, 43, 42, 32 }, // tile 14
        { 35, 36, 46, 45, 44, 34 }, // tile 15
        { 40, 41, 49, 48, 47, 39 }, // tile 16
        { 42, 43, 51, 50, 49, 41 }, // tile 17
        { 44, 45, 53, 52, 51, 43 } // tile 18
    };

    // private static final int[][] VERTICES = {
    // {0, 1, 2, 8, 9, 10}, // tile 0
    // {2, 3, 4, 10, 11, 12}, // tile 1
    // {4, 5, 6, 12, 13, 14}, // tile 2
    // {7, 8, 9, 17, 18, 19}, // tile 3
    // {9, 10, 11, 19, 20, 21}, // tile 4
    // {11, 12, 13, 21, 22, 23}, // tile 5
    // {13, 14, 15, 23, 24, 25}, // tile 6
    // {16, 17, 18, 27, 28, 29}, // tile 7
    // {18, 19, 20, 29, 30, 31}, // tile 8
    // {20, 21, 22, 31, 32, 33}, // tile 9
    // {22, 23, 24, 33, 34, 35}, // tile 10
    // {24, 25, 26, 35, 36, 37}, // tile 11
    // {28, 29, 30, 38, 39, 40}, // tile 12
    // {30, 31, 32, 40, 41, 42}, // tile 13
    // {32, 33, 34, 42, 43, 44}, // tile 14
    // {34, 35, 36, 44, 45, 46}, // tile 15
    // {39, 40, 41, 47, 48, 49}, // tile 16
    // {41, 42, 43, 49, 50, 51}, // tile 17
    // {43, 44, 45, 51, 52, 53} // tile 18 };

    public static final int[][] TileAdjacency = {
            { 1, 3, 4 }, // 0
            { 0, 2, 4, 5 }, // 1
            { 1, 5, 6 }, // 2
            { 0, 4, 7, 8 }, // 3
            { 0, 1, 3, 5, 8, 9 }, // 4
            { 1, 2, 4, 6, 9, 10 }, // 5
            { 2, 5, 10, 11 }, // 6
            { 3, 8, 12 }, // 7
            { 3, 4, 7, 9, 12, 13 }, // 8
            { 4, 5, 8, 10, 13, 14 }, // 9
            { 5, 6, 9, 11, 14, 15 }, // 10
            { 6, 10, 15 }, // 11
            { 7, 8, 13, 16 }, // 12
            { 8, 9, 12, 14, 16, 17 }, // 13
            { 9, 10, 13, 15, 17, 18 }, // 14
            { 10, 11, 14, 18 }, // 15
            { 12, 13, 17 }, // 16
            { 13, 14, 16, 18 }, // 17
            { 14, 15, 17 } // 18
    };

    // the two vertices that each road connects; 72 roads total; makes a graph
    public static final int[][] RoadConnections = {
        {0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, // 1st line
        {0, 8}, {2, 10}, {4, 12}, {6, 14}, // 1st verticals
        {7, 8}, {8, 9}, {9, 10}, {10, 11}, {11, 12}, {12, 13}, {13, 14}, {14, 15}, // 2nd line
        {7, 17}, {9, 19}, {11, 21}, {13, 23}, {15, 25}, // 2nd verticals
        {16, 17}, {17, 18}, {18, 19}, {19, 20}, {20, 21}, {21, 22}, {22, 23}, {23, 24}, {24, 25}, {25, 26}, // 3rd line
        {16, 27}, {18, 29}, {20, 31}, {22, 33}, {24, 35}, {26, 37}, // 3rd verticals
        {27, 28}, {28, 29}, {29, 30}, {30, 31}, {31, 32}, {32, 33}, {33, 34}, {34, 35}, {35, 36}, {36, 37}, // 4th line
        {28, 38}, {30, 40}, {32, 42}, {34, 44}, {36, 46}, // 4th verticals
        {38, 39}, {39, 40}, {40, 41}, {41, 42}, {42, 43}, {43, 44}, {44, 45}, {45, 46}, // 5th line
        {39, 47}, {41, 49}, {43, 51}, {45, 53},  // 5th verticals
        {47, 48}, {48, 49}, {49, 50}, {50, 51}, {51, 52}, {52, 53}  // 6th (final) line
    };

    public static final int[][] PortVertices = {
        {0, 1},     // port 0
        {5, 4},     // port 1
        {14, 15},   // port 2
        {26, 37},   // port 3
        {45, 46},   // port 4
        {50, 51},   // port 5
        {47, 48},   // port 6
        {28, 38},   // port 7
        {7, 17}     // port 8
    };


    public static ArrayList<Integer> getAdjacentVertices(int vertex) { // helper function, used in board setup
        ArrayList<Integer> connections = new ArrayList<>();

        for (int[] edge : AdjacencyMaps.RoadConnections) {
            if (edge[0] == vertex) {
                connections.add(edge[1]);
            }
            if (edge[1] == vertex) {
                connections.add(edge[0]);
            }
        }

        return connections;
    }
}