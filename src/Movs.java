/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Clase dummy con movimientos
 * @author Awake
 */
public class Movs {
    public static final String MOV_N = "moveN";
    public static final String MOV_NE = "moveNE";
    public static final String MOV_E = "moveE";
    public static final String MOV_SE = "moveSE";
    public static final String MOV_S = "moveS";
    public static final String MOV_SW = "moveSW";
    public static final String MOV_W = "moveW";
    public static final String MOV_NW = "moveNW";
    
    public static final int INT_MOV_N = 1;
    public static final int INT_MOV_NE = 2;
    public static final int INT_MOV_E = 5;
    public static final int INT_MOV_SE = 8;
    public static final int INT_MOV_S = 7;
    public static final int INT_MOV_SW = 6;
    public static final int INT_MOV_W = 3;
    public static final int INT_MOV_NW = 0;
    
    public static final String ESPERA = "moveN";
    public static final String REFUEL = "refuel";
    public static final String CHECK = "checkin";
    public static final String PERCEIVE = "perceive";
    public static final String TRACE = "trace";
    
    
    
    public static String intToString(int max_cont){
        String mov = Movs.ESPERA;
        switch(max_cont){
            case Movs.INT_MOV_N:
                mov = Movs.MOV_N;
                break;
            case Movs.INT_MOV_NE:
                mov = Movs.MOV_NE;
                break;
            case Movs.INT_MOV_E:
                mov = Movs.MOV_E;
                break;
            case Movs.INT_MOV_SE:
                mov = Movs.MOV_SE;
                break;
            case Movs.INT_MOV_S:
                mov = Movs.MOV_S;
                break;
            case Movs.INT_MOV_SW:
                mov = Movs.MOV_SW;
                break;
            case Movs.INT_MOV_W:
                mov = Movs.MOV_W;
                break;
            case Movs.INT_MOV_NW:
                mov = Movs.MOV_NW;
                break;
                
        }
        return mov;
    }
}
