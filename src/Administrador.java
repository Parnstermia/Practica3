/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.ArrayList;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author Awake
 */
public class Administrador extends SingleAgent{
    private static final int ESTADO_SUBSCRIPCION = 0;
    private static final int ESTADO_CHECK = 1;
    private static final int ESTADO_EXPLORAR = 2;
    private static final int ESTADO_ENCONTRADO = 3;
    private static final int ESTADO_FIN = 4;
    private static final int ESTADO_ERROR = -1;
    
    private ArrayList<AgentID> agentes;
    private HashMap<String,Integer> baterias;
    private HashMap<String,String> tipos;
    private Mapa mapa;
    private String nivel;
    private String host;
    private boolean ejecutar = true;
    private int estado;
    
    private String conversationID;
    private String inReplyTo;
    private int performative;
    
    private int contador = 0;
    
    /**
     * @author Sergio López Ayala
     * @brief Constructor del agente Administrador
     * @param aid Identificador propio del agente
     * @param host Nombre del virtualhost objetivo
     * @param nivel Nombre del mapa al que suscribirse
     * @param aids ArrayList con los identificadores de los agentes-subdito
     * @throws Exception
     */
    public Administrador(AgentID aid, String host, String nivel, ArrayList<AgentID> aids) throws Exception {
        super(aid);
        this.nivel = nivel;
        this.host = host;
        agentes = aids;
        baterias = new HashMap<>();
        tipos = new HashMap<>();
        for(AgentID agente: aids){
            baterias.put(agente.getLocalName(), 0);
        }
        
        estado = ESTADO_SUBSCRIPCION;
        //estado = ESTADO_FIN;
        mapa = new Mapa( 500 ,aids);
        
        
    }
    
    @Override
    public void execute(){
        JsonObject objeto;
        String movimiento;
        boolean exito = false;
        
        while(ejecutar){
            switch(estado){
                case ESTADO_SUBSCRIPCION:
                    objeto = new JsonObject();
                    objeto.add("world", nivel);
                    performative = ACLMessage.SUBSCRIBE;
                    enviarMensaje(objeto, new AgentID(host), performative, null);
                    exito = recibirMensaje();
                    if(exito){
                        System.out.println("\nExito en el subscribe\nClave de sesión: " + conversationID);
                        estado = ESTADO_CHECK;
                    }else{
                        System.out.println("Fallo en el subscribe");
                        ejecutar = false;
                    }
                    break;
                case ESTADO_CHECK:
                    performative = ACLMessage.REQUEST;
                    objeto = new JsonObject();
                    objeto.add("orden", "checkin");
                    
                    System.out.println("NUMERO de agentes: " + agentes.size());
                    for(AgentID id: agentes){
                        enviarMensaje(objeto, id, performative, inReplyTo);
                        recibirMensaje();
                    }
                    estado = ESTADO_EXPLORAR;
                    break;
                case ESTADO_ERROR:
                    System.out.println("Se ha producido un error");
                    System.out.println("Host: " + host);
                    System.out.println("Mundo: " + nivel);
                    System.out.println("Performativa: "  + performative);
                    break;

                case ESTADO_EXPLORAR:
                    AgentID agente;
                    String tipo;
                    
                    for(int i = 0; i < agentes.size(); i++){
                        contador++; // borrar luego
                        agente = agentes.get(i);
                        tipo = tipos.get(agente.getLocalName());
                        System.out.println(agente.getLocalName() + ", bateria :" + baterias.get(agente.getLocalName()));
                        if(baterias.get(agente.getLocalName()) < 3){
                            objeto = new JsonObject();
                            
                            
                            if( mapa.checkRefuel())
                                movimiento = Movs.REFUEL;
                            else
                                movimiento = Movs.ESPERA;
                            
                            System.out.println("Solicitud refuel o espera");
                            
                            objeto.add("orden", movimiento);
                            performative = ACLMessage.REQUEST;
                            enviarMensaje(objeto, agente, performative, null);
                            
                            if(movimiento.equals(Movs.REFUEL)){
                                recibirMensaje();
                            }
                            
                        }else{
                            objeto = new JsonObject();
                            
                            movimiento = mapa.elegirMovimiento(agente, tipo);
                            System.out.println(movimiento);
                            if(!movimiento.equals(Movs.ESPERA) && !movimiento.equals(Movs.PERCEIVE)){
                                baterias.put(agente.getLocalName(), baterias.get(agente.getLocalName())-1);
                            }
                            objeto.add("orden", movimiento);
                            performative = ACLMessage.REQUEST;
                            enviarMensaje(objeto, agente, performative, null);
                            
                        }
                        if( movimiento.equals(Movs.PERCEIVE)){
                            recibirMensaje();
                        }
                        try{
                            //Thread.sleep(1000);
                        
                            // borrar luego
                            if(contador >= 60){
                                System.out.println("No se ha encontrado el objetivo");
                                estado = ESTADO_FIN;
                            }
                        
                        }catch(Exception e){}
                    }
                    
                    
                    break;
                case ESTADO_ENCONTRADO:
                    for(int i = 0; i < agentes.size(); i++){
                        agente = agentes.get(i);
                        tipo = tipos.get(agente.getLocalName());
                        if(baterias.get(agente.getLocalName()) < 3){
                            objeto = new JsonObject();
                            
                            if( mapa.checkRefuel())
                                movimiento = Movs.REFUEL;
                            else{
                                movimiento = Movs.ESPERA;
                                estado = ESTADO_FIN; // cambiar luego
                            }
                            objeto.add("orden", movimiento);
                            performative = ACLMessage.REQUEST;
                            
                            enviarMensaje(objeto, agente, performative, null);
                            
                             if(movimiento.equals(Movs.REFUEL)){
                                recibirMensaje();
                            }
                            
                        }else{
                            objeto = new JsonObject();
                            
                            
                            movimiento = mapa.elegirMovimiento(agente, tipo);
                            objeto.add("orden", movimiento);
                            performative = ACLMessage.REQUEST;
                            
                            enviarMensaje(objeto, agente, performative, null);
                            
                        }
                        
                        
                    }
                    break;
                case ESTADO_FIN:
                    System.out.println("CANCELANDO...");
                    objeto = new JsonObject();
                    performative = ACLMessage.CANCEL;
                    for(int i = 0 ; i < agentes.size(); i++){
                        enviarMensaje(objeto, agentes.get(i), performative, null);
                    }
                    enviarMensaje(objeto, new AgentID(host), performative, null);
                    
                    recibirMensaje(); //agree
                    recibirMensaje(); //trace
                    ejecutar=false;
                    
                    break;
            }
        }
    }
    
    public boolean recibirMensaje(){
        boolean exito = true;
        ACLMessage inbox;
        try {
            inbox = receiveACLMessage();
            JsonObject json = Json.parse(inbox.getContent()).asObject();
            
            // Resultado del checkin
            if( json.get("orden") != null){
                if( json.get("orden").asString().equals(Movs.CHECK)){
                    System.out.println("Parseando check in");
                    
                    JsonObject json2 = json.get("capabilities").asObject();

                    int fuelRate = json2.get("fuelrate").asInt();
                    int range = json2.get("range").asInt();
                    boolean fly = json2.get("fly").asBoolean();

                    String tipo;
                    if( fly )
                        tipo = vehiculo.TIPO_VOLADOR;
                    else
                        if ( fuelRate == 4 )
                            tipo = vehiculo.TIPO_CAMION;
                        else
                            tipo = vehiculo.TIPO_COCHE;

                    tipos.put(inbox.getSender().getLocalName(), tipo);
                    
                    System.out.println("Tipo asignado: " + tipo);
                }else if ( json.get("orden").asString().equals(Movs.PERCEIVE)){
                    
                    mapa.updateMap(inbox.getSender(), json);
                    
                }else if ( json.get("orden").asString().equals(Movs.REFUEL) ){
                    System.out.println("Refuel confirmado"); // borrar
                    
                    baterias.put(inbox.getSender().getLocalName(), 100);
                
                }
            }else if( json.get("result") != null){
                System.out.println("Recibo el converID");
                conversationID = inbox.getConversationId();
                
            }else if( json.get("trace") != null ){
                try{
                    JsonArray ja = json.get("trace").asArray();
                    byte data[] = new byte[ja.size()];
                    for(int i=0; i<data.length; i++){
                        data[i]=(byte) ja.get(i).asInt();
                    }
                    String title;
                    title = String.format("traza de "+conversationID+".png");
                    FileOutputStream fos = new FileOutputStream(title);
                    fos.write(data);
                    fos.close();
                    System.out.println("Traza Guardada como: "+title);
                }catch(IOException e){

                }
                ejecutar = false;
            }else if(inbox.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD){
                System.out.println("Mensaje no reconocido");
                String details = json.get("details").asString();
                System.out.println(details);
                exito = false;
            }else if(inbox.getPerformativeInt() == ACLMessage.FAILURE){
                System.out.println("Failure");
                String details = json.get("details").asString();
                System.out.println(details);
                exito = false;
            }else{
                System.out.println("Otro mensaje diferente");
                
                System.out.println(json.toString());
                exito = false;
            }
            
        }catch(Exception e){
            
        }
        return exito;
    }
    
    /**
     * @author Sergio López Ayala
     * @param objeto Json a enviar
     * @param receiver AgentID receptor del mensaje
     * @param performative Performativa a utilizar
     * @param conversationID ID de la sesión
     * @param inReplyTo Campo de en respuesta de
     */
    public void enviarMensaje(JsonObject objeto, AgentID receiver, int performative, String inReplyTo){
        ACLMessage outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(receiver);
        outbox.setContent(objeto.toString());
        if( conversationID != null)
            outbox.setConversationId(conversationID);
        if( inReplyTo != null)
            outbox.setInReplyTo(inReplyTo);
        outbox.setPerformative(performative);
        
        this.send(outbox);
    }
}
