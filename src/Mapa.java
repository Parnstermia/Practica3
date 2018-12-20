
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.Stack;

/**
 * 
 * @author Sergio López Ayala
 */
public class Mapa {
    public static final int CELDA_LIBRE = 0;
    public static final int OBSTACULO = 1;
    public static final int BORDE_DEL_MUNDO = 2;
    public static final int OBJETIVO = 3;
    public static final int VEHICULO = 4;
    public static final int DESCONOCIDO = 5;
    public static final int OUT_OF_BOUNDS = 6;
    
    private int[][] matriz;
    private int miDimension; //10 = 10x10
    private int bateriaGlobal;
    private boolean finalEncontrado;
    Posicion objetivo;
    
    private HashMap<String, Posicion> vehiculos;
    private HashMap<String, Integer> valor_en_Mapa;
    
    public Mapa(int dimension, ArrayList<AgentID> aids){
        miDimension = dimension;
        matriz = new int[miDimension][miDimension];
        Arrays.fill(matriz, DESCONOCIDO);
        vehiculos = new HashMap<>();
        setValoresAgentes(aids);
        bateriaGlobal = 10000;
    }
    
 
    public Mapa(ArrayList<AgentID> aids){
        vehiculos = new HashMap<>();
        miDimension = 10;
        matriz = new int[miDimension][miDimension];
        Arrays.fill(matriz, DESCONOCIDO);
        setValoresAgentes(aids);
        bateriaGlobal = 10000;
    }
    
       private void setValoresAgentes(ArrayList<AgentID> aids){
        int i = 7;
        valor_en_Mapa = new HashMap<>();
        for(AgentID aid: aids){
            valor_en_Mapa.put(aid.getLocalName(), i);
            vehiculos.put(aid.getLocalName(), new Posicion(20,20));
            i++;
        }
    }
    
    public int get(int x, int y)throws IndexOutOfBoundsException{
        if ( x < miDimension && y < miDimension && x >= 0 && y >= 0){
            return(matriz[x][y]);
        }else{
            return OUT_OF_BOUNDS;
        }
    }
    
    public int get(Posicion pos){
        if ( pos.x < miDimension && pos.y< miDimension && pos.x >= 0 && pos.y >= 0){
            return(matriz[pos.x][pos.y]);
        }else{
            return OUT_OF_BOUNDS;
        }
    }
    public void set(Posicion pos, int val)throws IndexOutOfBoundsException{
        if ( pos.x < miDimension && pos.y < miDimension && pos.x >= 0 && pos.y >= 0){
            matriz[pos.x][pos.y] = val;
        }else if ( pos.x > miDimension || pos.y > miDimension){
            redimensionar(miDimension*2);
            matriz[pos.x][pos.y] = val;
        }
    }
    
    public void redimensionar(int nuevaDimension){
        if(nuevaDimension > miDimension){
            int[][] nuevaMatriz = new int[nuevaDimension][nuevaDimension];
            
            //Rellenamos de casillas desconocidas
            for(int i = 0; i < nuevaDimension; i++){
                for(int j = 0; j < nuevaDimension; j++){
                    nuevaMatriz[i][j] = DESCONOCIDO;
                }
            }
            
            for(int i = 0; i < miDimension;i++){
                for(int j = 0; j < miDimension; j++){
                    nuevaMatriz[i][j] = matriz[i][j];
                }
            }
            
            matriz = nuevaMatriz;
            miDimension = nuevaDimension;
        }
    }
    
    public int getDimension(){
        return miDimension;
    }
    public boolean checkRefuel(){
        return (bateriaGlobal > 0);
    }

    public Posicion getPosicionVehiculo(AgentID aid){
        return vehiculos.get(aid.getLocalName());
    }
    
    public int getBateriaGlobal(){
        return bateriaGlobal;
    }
    
    // return bateria del vehiculo con AgentID aid
    public boolean updateMap(AgentID aid, JsonObject percepcion){
        Posicion posActual = new Posicion();
        boolean objetivo_encontrado = false;
        
       
        //Posición del agente. 
        if( vehiculos.containsKey(aid.getLocalName())){
            posActual = vehiculos.get(aid.getLocalName());
        }
        
        if(percepcion.get("x") != null){
            int x,y;
            x = percepcion.get("x").asInt();
            y = percepcion.get("y").asInt();
            posActual = new Posicion(x,y);
            vehiculos.put(aid.getLocalName(), posActual);
        }
        
        if(percepcion.get("sensor") != null){
            int contador = 0;
            JsonArray json = percepcion.get("sensor").asArray();
            ArrayList<Integer> miRadar = new ArrayList(json.size());
            for (JsonValue j : json){
                miRadar.add(contador, j.asInt());
                contador++;
            }
            int tam = (int) Math.floor( Math.sqrt(json.size()) );
            int offset = (tam-1)/2;
            Posicion topleft = new Posicion(posActual.x-offset, posActual.y-offset);

            contador = 0;
            for(int i = topleft.x ; i < tam; i++){
                for(int j = topleft.y; j < tam ; j++){
                    int value = miRadar.get(contador);
                    if( value == OBJETIVO){
                        this.objetivo = new Posicion(i,j);
                    }
                    //System.out.println(pos); //borrar luego
                    set(new Posicion(i,j), miRadar.get(contador));
                }
                contador++;
            }
            //System.out.println("Contadas:" + i);
        }
        
        if( percepcion.get("energy") != null){
            bateriaGlobal = percepcion.get("energy").asInt();
        }
        
        if ( percepcion.get("goal") != null){
            objetivo_encontrado = percepcion.get("goal").asBoolean();
            if(objetivo_encontrado){
                finalEncontrado = true;
            }
        }
        
        if( percepcion.get("battery") != null){
            int bateriaCoche = percepcion.get("battery").asInt();
        }
        
        return objetivo_encontrado;
    }

    
    public Posicion siguientePosicion(Posicion actual, String mov){
        Posicion sig = actual;
        
        switch(mov){
            case Movs.MOV_N:
                sig = new Posicion(actual.x,actual.y-1);
                break;
            case Movs.MOV_NE:
                sig = new Posicion(actual.x+1,actual.y-1);
                break;
            case Movs.MOV_E:
                sig = new Posicion(actual.x+1,actual.y);
                break;
            case Movs.MOV_SE:
                sig = new Posicion(actual.x+1,actual.y+1);
                break;
            case Movs.MOV_S:
                sig = new Posicion(actual.x,actual.y+1);
                break;
            case Movs.MOV_SW:
                sig = new Posicion(actual.x-1,actual.y+1);
                break;
            case Movs.MOV_W:
                sig = new Posicion(actual.x-1,actual.y);
                break;
            case Movs.MOV_NW:
                sig = new Posicion(actual.x-1,actual.y-1);
                break;
            default:
                sig = actual;
                break;
        }
        return sig;
    }
    
    public String elegirMovimiento(AgentID aid, String tipo){
        String movimiento = Movs.ESPERA;
        
        if( vehiculos.containsKey(aid.getLocalName())){
            Posicion posActual = new Posicion(vehiculos.get(aid.getLocalName()));

            if(!finalEncontrado){       //Exploración
                switch(tipo){
                    case vehiculo.TIPO_VOLADOR:
                        movimiento = checkCercania(posActual, 3, aid, true);
                        break;
                    case vehiculo.TIPO_COCHE:
                        movimiento = checkCercania(posActual, 7, aid, false);

                        break;
                    case vehiculo.TIPO_CAMION:
                        movimiento = checkCercania(posActual, 11, aid, false);

                        break;
                }
            }else{                      //Greedy hasta el objetivo
                movimiento = toObjective(posActual, aid, false);
            }
            
            Posicion sig = new Posicion();
            sig.clone(siguientePosicion(posActual, movimiento));

            System.out.println("Agente : " + aid.getLocalName() + ", movimiento: " + movimiento + posActual);

            vehiculos.put(aid.getLocalName() ,sig);
        }else{
            movimiento = Movs.PERCEIVE;
        }
        return movimiento;
    }
    
        /*
    * @brief Comprueba si la casilla es un obstaculo
    * @author Sergio López Ayala
    * @param pos posición a consultar
    * @param fly capacidad del agente de sobrevolarlo
    * @return boolean true si la casilla es un obstaculo, false si no lo es
    */
    public boolean checkObstaculo(Posicion pos, boolean fly){
        int value = get(pos);
        boolean no_pasable = ( value == BORDE_DEL_MUNDO || value == VEHICULO);
        
        if(fly)
            return no_pasable;
        else
            return (no_pasable || ( value == OBSTACULO));
    }
    
    /*
    * @brief Comprueba si un agente ha estado en una casilla
    * @author Sergio López Ayala
    * @param pos posición a consultar
    * @param id identificador del agente
    * @return boolean true si la ha vistado, false si no la ha vistado.
    */
    public boolean checkVisitada(Posicion pos, AgentID id){
        return (get(pos) == valor_en_Mapa.get(id.getLocalName()));
    }
    
    public String toObjective(Posicion posActual, AgentID id, boolean fly){
        String mov = Movs.ESPERA;
        
        int offset = 1;
        Posicion supIzq = new Posicion(posActual);
        supIzq.x -= offset;
        supIzq.x -= offset;
        Posicion pos = new Posicion(supIzq);
        
        int contador = 0;
        int max_cont = -1;
        double distancia = 123123;
        double menor_distancia = 123123;
        Stack<Integer> movimientos = new Stack();
        for(int i = 0 ; i < 3; i++){
            for(int j = 0; j < 3; j++){
                distancia = pos.getDistancia(this.objetivo);
                if(distancia <= menor_distancia){
                    if( checkObstaculo(pos,fly) && !checkVisitada(pos, id)){
                        menor_distancia = distancia;
                        max_cont = contador;  //orden a realizar
                        movimientos.add(max_cont);
                    }
                }
                pos.x++;
                
            }
            pos.x = supIzq.x;
            pos.y++;
            contador++;
        }
        
        // Transforma el entero en la orden String
        mov = Movs.intToString(max_cont);
        
        
        return mov;
    }
    
    /*
    * Comprueba si la posición no es un muro y tiene alrededor al menos un obstaculo
    */
    public boolean casillaJuntoAMuro(Posicion pos){
        boolean casilla = true;
        
        if( checkObstaculo(pos, false)){
            casilla = false;
        }else{
            for(int i = pos.x; i < 3; i++){
                for(int j = pos.y; j < 3 ; j++){
                    if( get(i,j) == OBSTACULO)
                        casilla = true;
                }
            }
        }
        return casilla;
    }
    
    public String checkCercania(Posicion posActual, int rango,AgentID aid, boolean fly){
        //NOrte sumas Y, Este Sumas X
         // Miramos Norte
         
         String mov =Movs.MOV_E;
        int x=posActual.x;
        int y=posActual.y;
        Posicion posAux= new Posicion(posActual);
        int puntosN=0;
        int puntosNE=0;
        int puntosNW=0;
        int puntosE=0;
        int puntosW=0;
        int puntosSE=0;
        int puntosSW=0;
        int puntosS=0;
        
        int [] puntos = new int[8];
        //Miramos norte
        for(int i=0; i<rango+5; i++){
             if(get(x,y-i)==CELDA_LIBRE){
                puntosN++;
            }
            else if(get(x,y-i)==DESCONOCIDO){
                puntosN=puntosN+10;
            }
            else if(checkObstaculo(new Posicion(x,y-i), fly)){
                puntosN=puntosN-1000;
            }
        }
        
        for(int i=0; i<rango+5; i++){
            if(get(x+i,y-i)==CELDA_LIBRE){
               puntosNE++;
           }
           else if(get(x+i,y-i)==DESCONOCIDO){
               puntosNE=puntosNE+10;
           }
           else if(checkObstaculo(new Posicion(x+i,y-i), fly)){
               puntosNE=puntosNE-1000;
           }
        }
         for(int i=0; i<rango+5; i++){
            if(get(x-i,y-i)==CELDA_LIBRE){
               puntosNW++;
           }
           else if(get(x-i,y-i)==DESCONOCIDO){
               puntosNW=puntosNW+10;
           }
           else if(checkObstaculo(new Posicion(x-i,y-i), fly)){
               puntosNW=puntosNW-1000;
           }
        }
        
        for(int i=0; i<rango+5; i++){
             if(get(x,y+i)==CELDA_LIBRE){
                puntosS++;
            }
            else if(get(x,y+i)==DESCONOCIDO){
                puntosS=puntosS+10;
            }
            else if(checkObstaculo(new Posicion(x,y+i), fly)){
                puntosS=puntosS-1000;
            }
        }
        
         for(int i=0; i<rango+5; i++){
            if(get(x+i,y+i)==CELDA_LIBRE){
               puntosSE++;
           }
           else if(get(x+i,y+i)==DESCONOCIDO){
               puntosSE=puntosSE+10;
           }
           else if(checkObstaculo(new Posicion(x+i,y+i), fly)){
               puntosSE=puntosSE-1000;
           }
        }
         for(int i=0; i<rango+5; i++){
            if(get(x-i,y+i)==CELDA_LIBRE){
               puntosSW++;
           }
           else if(get(x-i,y+i)==DESCONOCIDO){
               puntosSW=puntosSW+10;
           }
           else if(checkObstaculo(new Posicion(x-i,y+i), fly)){
               puntosSW=puntosSW-1000;
           }
        }
        
        for(int i=0; i<rango+5; i++){
             if(get(x+i,y)==CELDA_LIBRE){
                puntosE++;
            }
            else if(get(x+i,y)==DESCONOCIDO ){
                puntosE=puntosE+10;
            }
            else if(checkObstaculo(new Posicion(x+i,y), fly)){
                puntosE=puntosE-1000;
            }
        }
         for(int i=0; i<rango+5; i++){
             if(get(x-i,y)==CELDA_LIBRE){
                puntosW++;
            }
            else if(get(x-i,y)==DESCONOCIDO ){
                puntosW=puntosW+10;
            }
            else if(checkObstaculo(new Posicion(x+i,y), fly)){
                puntosW= puntosW-1000;
            }
        }
         
         if(puntosS>puntosN && puntosS> puntosE && puntosS > puntosW ){
             mov=Movs.MOV_S;
         }
         else if(puntosN>puntosS && puntosN> puntosE && puntosN > puntosW){
             mov=Movs.MOV_N;
         }
          else if(puntosE>puntosS && puntosE> puntosN && puntosE > puntosW){
             mov=Movs.MOV_E;
         }
         else if(puntosW>puntosS && puntosW> puntosN && puntosW > puntosW){
             mov=Movs.MOV_W;
         }
       
             
       
        
        
        
        
        return mov;
            
           
        /*
        int maxDesconocidas = -1;
        String mov = Movs.MOV_NW;
        int offset = 1;
        Posicion supIzq = new Posicion(posActual);
        supIzq.x -= offset;
        supIzq.x -= offset;
        Posicion pos = new Posicion(supIzq);
        
        int desconocidas;
        int contador = 0;
        int max_cont = -1;
        System.out.println("Posicion actual" + posActual.toString() + " rango: "+ rango);
        ArrayList<Integer> no_visitados = new ArrayList();
        ArrayList<Integer> con_muro = new ArrayList();
        
        for(int i = 0 ; i < 3; i++){
            for(int j = 0; j < 3; j++){
                if(checkObstaculo(pos, fly)){
                    if(!checkVisitada(pos, aid)){
                        no_visitados.add(contador);
                    }
                    if( no_visitados.isEmpty() && con_muro.isEmpty()){
                        if( casillaJuntoAMuro(pos)){
                            con_muro.add(contador);
                        }
                    }
                }
                pos.x++;
                
            }
            pos.x = supIzq.x;
            pos.y++;
            contador++;
        }
        // Transforma el entero en la orden String
        if( casillasPorDescubrir(posActual, rango) > (rango*rango/2)){
            mov = Movs.PERCEIVE;
        }else{
            if(no_visitados.isEmpty()){
                if (!con_muro.isEmpty())
                    mov = Movs.intToString(con_muro.get(0));
                
            }else
                mov = Movs.intToString(no_visitados.get(0));
            
        }
        
        return mov;
        */
    }
    
    /*
    * @author Sergio López Ayala
    * @brief Cuenta las casillas cercanas desconocidas
    * @param rango define la distancia de casillas posibles a descubrir
    * @param pos posición a consultar
    */
    private int casillasPorDescubrir(Posicion pos, int rango){
        int casillasDesconocidas = 0;
        
        int offset = (rango-1)/2;
        Posicion supIzq = new Posicion(pos.x-offset, pos.y-offset);
        
        for(int i = supIzq.x ; i < rango; i++){
            for(int j = supIzq.y; j < rango; j++){
                if( get(pos) == DESCONOCIDO){
                    casillasDesconocidas++;
                }
            }
        }
        return casillasDesconocidas;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Arrays.hashCode(this.matriz);
        hash = 59 * hash + this.miDimension;
        return hash;
    }
    
}
