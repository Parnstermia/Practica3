/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p3v2;


import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author Sergio López Ayala
 */
public class vehiculo extends SingleAgent{
    public static final String TIPO_VOLADOR = "volador";
    public static final String TIPO_CAMION = "camion";
    public static final String TIPO_COCHE = "coche";
    
    private AgentID mastermind;
    private AgentID host;
    private AgentID miID;
    private String conversationID;
    private String inReplyTo;
    
    private boolean ejecutar;
    
    /**
     * Constructor del Agente vehículo
     * @param aid Identificador del agente de tipo AgentID
     * @param host Nombre del virtualhost objetivo
     * @param mastermind Identificador del agente Administrador
     * @throws Exception Fallo en la creación de agentes
     */
    public vehiculo(AgentID aid, String host, AgentID mastermind) throws Exception {
        super(aid);
        this.host = new AgentID(host);
        ejecutar = true;
        this.mastermind = mastermind;
        miID = aid;
    }
    
    /**
     *
     * @param objeto Json a enviar
     * @param receiver Identificador del agente objetivo
     * @param performative Tipo de performativa del mensaje
     * @param conversationID Identificador de conversación
     * @param inReplyTo Identificador del mensaje anterior del agente
     */
    public void enviarMensaje(JsonObject objeto, AgentID receiver, int performative, String conversationID, String inReplyTo){
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
    
    
    /**
     * @author Sergio López Ayala
     * @brief método para tratar las comunicaciones del host
     * @param inbox mensaje con performativa INFORM
     */
public void gestionarInformes(ACLMessage inbox){
    JsonObject obj = Json.parse(inbox.getContent()).asObject();
    int performative;
    
    if ( obj.get("result") != null){
        System.out.println("Parseando resultados");
        if( obj.get("capabilities") != null){
            System.out.println("Recibido checkin");
            inReplyTo = inbox.getReplyWith();
            obj.add("orden", Movs.CHECK);
            performative = ACLMessage.INFORM;
            enviarMensaje(obj, mastermind, performative, conversationID, null);
            
            
        }else{
            JsonObject json2 = obj.get("result").asObject();
            if(json2.get("battery")!= null){
                json2.add("orden", Movs.PERCEIVE);
                performative = ACLMessage.INFORM;
                enviarMensaje(json2, mastermind, performative, conversationID, null);
            }
        }
        
        
    }else if ( obj.get("details") != null){
        System.out.println("Error agent: " + miID.getLocalName());
        System.out.println(obj.toString());
        ejecutar = false;
        
    }else if ( obj.get("trace") != null){
        try{
        JsonArray ja = obj.get("trace").asArray();
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
    }
    
}

    /**
     * @author Sergio López Ayala
     * @brief método para tratar las peticiones recibidas por el Administrador
     * @param inbox mensaje con performativa REQUEST
     */
    public void gestionarPeticiones(ACLMessage inbox){
    JsonObject obj = Json.parse(inbox.getContent()).asObject();
    int performative; 
    
    
    
    if( obj.get("orden") != null){
        String orden = obj.get("orden").asString();
        switch(orden){
            case Movs.CHECK:
                conversationID = inbox.getConversationId();
                obj = new JsonObject();
                obj.add("command", orden);
                performative = ACLMessage.REQUEST;
                System.out.println("Agente: " + miID.getLocalName() + " id-Conver: " + conversationID);
                if(conversationID != null)
                    
                    enviarMensaje(obj, host, performative, conversationID, inReplyTo);
                else
                    System.out.println("No tienes el converID" + conversationID);
                break;
            case Movs.REFUEL:
                obj = new JsonObject();
                obj.add("command", orden);
                performative = ACLMessage.REQUEST;
                
                enviarMensaje(obj, host, performative, conversationID, inReplyTo);
                break;
            
            case Movs.PERCEIVE:
                obj = new JsonObject();;
                performative = ACLMessage.QUERY_REF;
                
                enviarMensaje(obj, host, performative, conversationID, inReplyTo);
                break;
            
            default:
                obj = new JsonObject();
                obj.add("command", orden);
                performative = ACLMessage.REQUEST;
                System.out.println("Mensaje enviado\n perf: " + inbox.getPerformative());
                System.out.println("replyTo: " + inbox.getReplyTo());
                System.out.println("conver: " + inbox.getConversationId());
                System.out.println("orden: " + orden);
                enviarMensaje(obj, host, performative, conversationID, inReplyTo);
                
                break;
            
        }
    }
}
    
    /**
     * @author Sergio López Ayala
     * @brief método encargado de tratar los mensajes dependiendo de 
     * su performativa
     * @return exito variable de control, toma el valor false si se obtiene un
     * error
     */
    public boolean recibirMensaje(){
        boolean exito = true;
        ACLMessage inbox;
        try {
            inbox = receiveACLMessage();
            System.out.println(miID.getLocalName() + " recibe el mensaje con performativa " + inbox.getPerformative());
            int performative = inbox.getPerformativeInt();
            
            switch(performative){
                case ACLMessage.INFORM:
                    gestionarInformes(inbox);
                    
                    break;
                    
                    
                case ACLMessage.REQUEST:
                    gestionarPeticiones(inbox);
                    
                    break;
                    
                case ACLMessage.AGREE:
                    // Acepta el cancel
                    
                    break;
                    
                case ACLMessage.CANCEL:
                    System.out.println("Parando el proceso");
                    exito = false;
                    
                    break;
                case ACLMessage.NOT_UNDERSTOOD:
                    System.out.println("Not understood");
                    System.out.println(inbox.getContent().toString());
                    exito = false;
                    break;
                case ACLMessage.FAILURE:
                    System.out.println("Failure");
                    System.out.println(inbox.getContent().toString());
                    exito = false;
                    break;
            }
            
            
            
        }catch(InterruptedException e){
            System.err.println(e.toString());
        }
        
        return exito;
    }
    
    /**
     * @Author : Sergio López Ayala
     * @brief : bucle de ejecución del agente, termina si se encuentra un mensaje
     * de error como NOT_UNDERSTOOD, FAILURE o CANCEL
     */
    @Override
    public void execute(){
        while(ejecutar){
            ejecutar = recibirMensaje();
        }
    }
    
}
