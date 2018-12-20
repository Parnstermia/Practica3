/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Awake
 */
class Posicion {
    public int x,y;
    public Posicion(int x, int y){
        this.x = x;
        this.y = y;
    }
    public Posicion(Posicion other){
        this.x = other.x;
        this.y = other.y;
    }
    public Posicion(){
        x = 0;
        y = 0;
    }
    public double getDistancia(Posicion otro){
        return (Math.sqrt( (otro.x-x)*(otro.x-x)+(otro.y-y)*(otro.y-y) ) );
    }
    public void clone(Posicion otro){
        x = otro.x;
        y = otro.y;
    }
    @Override
    public boolean equals(Object obj){
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (!(obj instanceof Posicion))
            return false;
        Posicion other = (Posicion)obj;
        return other.x == x && 
               other.y == y;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + this.x;
        hash = 47 * hash + this.y;
        return hash;
    }
    
    public String toString(){
        return "X: " + Integer.toString(x) + ", Y: " + Integer.toString(y);
    }
}
