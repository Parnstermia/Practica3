
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;

import java.util.ArrayList;


/**
 *
 * @author 
 */
public class Main {

    public static void main(String[] args) {
        Administrador admin;
        String jefe = "MasterMindddd";
        String subdito = "peasantttt-";
        
        ////////////////////////////////////////////
        // No hacer push a los datos del grupo
        String virtualhost = "Bellatrix";
        String username = "Hercules";
        String pass = "Benavente";
        ////////////////////////////////////////////



        String nivel = "map1";
        
        AgentsConnection.connect("isg2.ugr.es",6000, virtualhost, username, pass, false);
        
        ArrayList<AgentID> aids = new ArrayList<>();
        for(int i = 0; i < 4; i ++)
            aids.add(new AgentID(subdito + i));
        
        
        try {

            admin = new Administrador(new AgentID(jefe), virtualhost, nivel, aids);
            for(int i = 0; i < 4; i++){
                vehiculo agent;
                agent = new vehiculo(aids.get(i), virtualhost, new AgentID(jefe));
                agent.start();
            }
            admin.start();

        } catch (Exception ex) {
            System.err.println("Error creando agentes");
            System.exit(1);
        }
    }
    
}