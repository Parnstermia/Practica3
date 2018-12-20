package p3v2;


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
    
    private int[] matriz;
    private int miDimension; //10 = 10x10
    private int nCeldas;
    private int bateriaGlobal;
    private boolean finalEncontrado;
    Posicion objetivo;
    
    private HashMap<String, Posicion> vehiculos;
    private HashMap<String, Integer> valor_en_Mapa;
    
    public Mapa(int dimension, ArrayList<AgentID> aids){
        miDimension = dimension;
        nCeldas = miDimension*miDimension;
        matriz = new int[nCeldas];
        Arrays.fill(matriz, DESCONOCIDO);
        vehiculos = new HashMap<>();
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
    public Mapa(ArrayList<AgentID> aids){
        vehiculos = new HashMap<>();
        miDimension = 10;
        nCeldas = miDimension*miDimension;
        matriz = new int[nCeldas];
        Arrays.fill(matriz, DESCONOCIDO);
        setValoresAgentes(aids);
        bateriaGlobal = 10000;
    }
    
    public int get(int x, int y)throws IndexOutOfBoundsException{
        if ( x < miDimension && y < miDimension && x >= 0 && y >= 0){
            return(matriz[x*miDimension + y]);
        }else{
            return OUT_OF_BOUNDS;
        }
    }
    
    public int get(int i)throws IndexOutOfBoundsException{
        if( i < nCeldas){
            return matriz[i];
        }else{
            throw new IndexOutOfBoundsException();
        }
    }
    public void set(int x, int y, int val)throws IndexOutOfBoundsException{
        if ( x < miDimension && y < miDimension && x >= 0 && y >= 0){
            matriz[x*miDimension+y] = val;
        }else if ( x > miDimension || y > miDimension){
            redimensionar(miDimension+miDimension/2);
            matriz[x*miDimension+y] = val;
        }
    }
    
    public void redimensionar(int nuevaDimension){
        if(nuevaDimension > miDimension){
            int nuevasCeldas = nuevaDimension*nuevaDimension;
            int[] nuevaMatriz = new int[nuevasCeldas];
            Arrays.fill(nuevaMatriz, 'D');
            
            int offset = 0;
            for(int i = 0; i < nCeldas; i++){
                if(i%miDimension == 0){
                    offset = i%miDimension;
                    offset *= (nuevaDimension-miDimension);
                }
                nuevaMatriz[i + offset] = matriz[i];
                
            }
            matriz = nuevaMatriz;
            miDimension = nuevaDimension;
            nCeldas = nuevasCeldas;
        }
    }
    
    public int getDimension(){
        return miDimension;
    }
    public int getCeldas(){
        return nCeldas;
    }
    
    public boolean checkRefuel(){
        return (bateriaGlobal > 0);
    }
    
    public ArrayList<ValorRLE> compresionRLE(){
        ArrayList<ValorRLE> mapaComprimido = new ArrayList<>();
        int anterior = matriz[0];
        int repeticiones = 0;
        for(int i = 0; i < nCeldas; i++){
            if(anterior == matriz[i]){
                repeticiones++;
            }else{
                mapaComprimido.add(new ValorRLE(anterior,repeticiones));
                anterior = matriz[i];
                repeticiones = 1;
            }
        }
        mapaComprimido.add(new ValorRLE(anterior,repeticiones));
        
        return mapaComprimido;
    }
    
    public void descompresionRLE(ArrayList<ValorRLE> mapaComprimido){
        int veces = 0;
        for(int i = 0 ; i < mapaComprimido.size(); i++){
            veces+= mapaComprimido.get(i).veces;
        }
        
        if( nCeldas < veces){
            redimensionar((int) Math.sqrt(veces));
        }
        
        int nuevaMatriz[] = new int[veces];
        int indice = 0;
        for(int i = 0 ; i < mapaComprimido.size(); i++){
            ValorRLE valor = mapaComprimido.get(i);
            for(int j = 0; j < valor.veces; j++, indice++)
                nuevaMatriz[indice] = valor.valor;
        }
        
        for(int i = 0 ; i < nCeldas ; i++){
            if(nuevaMatriz[i] == DESCONOCIDO && matriz[i] != DESCONOCIDO){
                nuevaMatriz[i] = matriz[i];
            }
        }
        matriz = nuevaMatriz;
        nCeldas = veces;
        miDimension = (int) Math.sqrt(veces);
    }
   
    @Override
    public boolean equals(Object obj){
        if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (!(obj instanceof Mapa))
                return false;
            Mapa other = (Mapa)obj;
            if(miDimension == other.getDimension()){
                boolean exito = true;
                for(int i = 0; i < nCeldas;i++){
                    if(matriz[i] != other.get(i)){
                        exito = false;
                    }
                }
                return exito;
            }else{
                return false;
            }
            
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
        
        if(percepcion.get("gps") != null){
            int x,y;
            x = percepcion.get("gps").asObject().get("x").asInt();
            y = percepcion.get("gps").asObject().get("y").asInt();
            posActual = new Posicion(x,y);
            vehiculos.put(aid.getLocalName(), posActual);
        }
        
        if(percepcion.get("radar") != null){
            int i = 0;
            JsonArray json = percepcion.get("radar").asArray();
            ArrayList<Integer> miRadar = new ArrayList(json.size());
            for (JsonValue j : json){
                miRadar.add(i, j.asInt());
                i++;
            }
            int tam = (int) Math.floor( Math.sqrt(json.size()) );
            int offset = (tam-1)/2;
            Posicion topleft = new Posicion();
            topleft.x = posActual.x - offset;
            topleft.y = posActual.y - offset;

            Posicion pos = new Posicion(topleft.x, topleft.y);
            i = 0;
            for(int k = 0 ; i < tam; k++){
                for(int j = 0; i < tam ; j++){
                    int value = miRadar.get(i);
                    if( value == OBJETIVO)
                        this.objetivo = new Posicion(pos);
                    set(pos.x, pos.y, miRadar.get(i));
                    pos.x++;
                }
                pos.x = topleft.x;
                pos.y++;
                i++;
            }
        }
        
        if( percepcion.get("bateriaGlobal") != null){
            bateriaGlobal = percepcion.get("bateriaGlobal").asInt();
        }
        
        if ( percepcion.get("objetivo") != null){
            objetivo_encontrado = percepcion.get("objetivo").asBoolean();
            if(objetivo_encontrado){
                finalEncontrado = true;
            }
        }
        
        if( percepcion.get("bateria") != null){
            int bateriaCoche = percepcion.get("bateria").asInt();
        }
        
        return objetivo_encontrado;
    }
    
    public JsonObject toJson(){
        JsonObject json = new JsonObject();
        ArrayList<ValorRLE> lista = compresionRLE();
        JsonArray vector = new JsonArray();
        for (Iterator<ValorRLE> iterator = lista.iterator(); iterator.hasNext();) {
            ValorRLE next = iterator.next();
            vector.add(next.valor);
            vector.add(next.veces);
        }
        
        return json;
    }
    
    
    public void parseJson(JsonArray arr){
        ArrayList<ValorRLE> lista = new ArrayList();
        for(int i = 0; i < arr.size(); i+= 2){
            lista.add(new ValorRLE(arr.get(i).toString().charAt(0), arr.get(i+1).asInt()));
        }
        
        descompresionRLE(lista);
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
                        movimiento = checkCercania(posActual, 3);
                        break;
                    case vehiculo.TIPO_COCHE:
                        movimiento = checkCercania(posActual, 7);

                        break;
                    case vehiculo.TIPO_CAMION:
                        movimiento = checkCercania(posActual, 11);

                        break;
                }
            }else{                      //Greedy hasta el objetivo
                movimiento = toObjective(posActual, aid);
            }
            Posicion sig = siguientePosicion(posActual, movimiento);

            System.out.println("Agente : " + aid.getLocalName() + ", movimiento: " + movimiento);

            vehiculos.put(aid.getLocalName() ,sig);
        }else{
            movimiento = Movs.PERCEIVE;
        }
        return movimiento;
    }
    
    public boolean checkObstaculo(Posicion pos){
        int value = get(pos.x, pos.y);
        return ( value != OBSTACULO && value != BORDE_DEL_MUNDO && value != VEHICULO);
    }
    
    public boolean checkVisitada(Posicion pos, AgentID id){
        return (get(pos.x, pos.y) == valor_en_Mapa.get(id.getLocalName()));
    }
    
    public String toObjective(Posicion posActual, AgentID id){
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
                    if( checkObstaculo(pos) && !checkVisitada(pos, id)){
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
    
    
    public String checkCercania(Posicion posActual, int rango){
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
        for(int i = 0 ; i < rango; i++){
            for(int j = 0; j < rango; j++){
                desconocidas = casillasPorDescubrir(pos, rango);
                if(desconocidas >= maxDesconocidas){
                    if( checkObstaculo(pos) ){
                        maxDesconocidas = desconocidas;
                        max_cont = contador;  //orden a realizar
                    }
                }
                pos.x++;
                
            }
            pos.x = supIzq.x;
            pos.y++;
            contador++;
        }
        // Transforma el entero en la orden String
        if(maxDesconocidas > ((rango*2)+1)){
            System.out.println("percepcion");
            mov = Movs.PERCEIVE;
        }else{
            System.out.println("Seleccion de movimiento");
            mov = Movs.intToString(max_cont);
        }
        return mov;
        
    }
    private int casillasPorDescubrir(Posicion posActual, int rango){
        int casillasDesconocidas = 0;
        
        int offset = (rango-1)/2;
        Posicion supIzq = new Posicion(posActual);
        supIzq.x -= offset;
        supIzq.x -= offset;
        Posicion pos = new Posicion(supIzq);
        
        for(int i = 0 ; i < rango; i++){
            for(int j = 0; j < rango; j++){
                if( get(pos.x, pos.y) == DESCONOCIDO){
                    casillasDesconocidas++;
                }
                pos.x++;
            }
            pos.x = supIzq.x;
            pos.y++;
        }
        return casillasDesconocidas;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Arrays.hashCode(this.matriz);
        hash = 59 * hash + this.miDimension;
        hash = 59 * hash + this.nCeldas;
        return hash;
    }
    public class ValorRLE{
        public int valor;
        public int veces;
        
        public ValorRLE(int c, int v){
            valor = c;
            veces = v;
        }
        
        @Override
        public boolean equals(Object obj){
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (!(obj instanceof ValorRLE))
                return false;
            ValorRLE other = (ValorRLE)obj;
            return other.valor == valor && 
                   other.veces == veces;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + this.valor;
            hash = 23 * hash + this.veces;
            return hash;
        }
    }
}
