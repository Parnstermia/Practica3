
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
            return(matriz[x*miDimension+y]);
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
    public int get(Posicion pos){
        if ( pos.x < miDimension && pos.y< miDimension && pos.x >= 0 && pos.y >= 0){
            return(matriz[pos.x*miDimension+pos.y]);
        }else{
            return OUT_OF_BOUNDS;
        }
    }
    public void set(int x, int y, int val)throws IndexOutOfBoundsException{
        if ( x < miDimension && y < miDimension && x >= 0 && y >= 0){
            matriz[x*miDimension+y] = val;
        }else if ( x > miDimension || y > miDimension){
            redimensionar(miDimension*2);
            matriz[x*miDimension+y] = val;
        }
    }
    
    public void redimensionar(int nuevaDimension){
        if(nuevaDimension > miDimension){
            int nuevasCeldas = nuevaDimension*nuevaDimension;
            int[] nuevaMatriz = new int[nuevasCeldas];
            Arrays.fill(nuevaMatriz, DESCONOCIDO);
            
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
        
        if(percepcion.get("x") != null){
            int x,y;
            x = percepcion.get("x").asInt();
            y = percepcion.get("y").asInt();
            posActual = new Posicion(x,y);
            vehiculos.put(aid.getLocalName(), posActual);
        }
        
        if(percepcion.get("sensor") != null){
            int i = 0;
            JsonArray json = percepcion.get("sensor").asArray();
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
            for(int k = 0 ; k < tam; k++){
                for(int j = 0; j < tam ; j++){
                    int value = miRadar.get(i);
                    if( value == OBJETIVO){
                        this.objetivo = new Posicion(pos);
                    }
                    //System.out.println(pos); //borrar luego
                    set(pos.x, pos.y, miRadar.get(i));
                    pos.x++;
                }
                pos.x = topleft.x;
                pos.y++;
                i++;
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
    
    public boolean checkObstaculo(Posicion pos, boolean fly){
        int value = get(pos);
        if(fly)
            return( value != BORDE_DEL_MUNDO && value != VEHICULO);
        else
            return ( value != OBSTACULO && value != BORDE_DEL_MUNDO && value != VEHICULO);
    }
    
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
    public boolean casillaJuntoAMuro(Posicion pos){
        Posicion otra = new Posicion(pos);
        otra.x -= 1;
        otra.y -= 1;
        
        if( !checkObstaculo(pos, false)){
            return false; // es un muro
        }
        for(int i = 0 ; i < 3 ; i++){
            for(int j = 0; j < 3; j++){
                if( !pos.equals(otra)){
                    if( !checkObstaculo(otra, false)){
                        return true; // pos no es muro y otra si es muro
                    }
                }
                    
                otra.x++;
            }
            
            otra.x = pos.x;
            otra.y++;
        }
        return false;
    }
    
    public String checkCercania(Posicion posActual, int rango,AgentID aid, boolean fly){
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
                if( get(pos) == DESCONOCIDO){
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
    
    public void show(){
        
        for(int i = 0; i < nCeldas; i++){
            for(int j = 0; j < miDimension; j++){
                System.out.print(get(i));
            }
            System.out.println("");
        }

    }
}
