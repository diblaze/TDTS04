import ChatApp.*;          // The package containing our stubs
import org.omg.CosNaming.*; // HelloClient will use the naming service.
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;     // All CORBA applications need these classes.
import org.omg.PortableServer.*;   
import org.omg.PortableServer.POA;

import java.util.Scanner; //For terminal input
 
class ChatCallbackImpl extends ChatCallbackPOA
{
    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public void callback(String notification)
    {
        System.out.println(notification);
    }
}

public class ChatClient
{
    static Chat chatImpl;
    
    public static void main(String args[])
    {
	try {
	    // create and initialize the ORB
	    ORB orb = ORB.init(args, null);

	    // create servant (impl) and register it with the ORB
	    ChatCallbackImpl chatCallbackImpl = new ChatCallbackImpl();
	    chatCallbackImpl.setORB(orb);

	    // get reference to RootPOA and activate the POAManager
	    POA rootpoa = 
		POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();
	    
	    // get the root naming context 
	    org.omg.CORBA.Object objRef = 
		orb.resolve_initial_references("NameService");
	    NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
	    
	    // resolve the object reference in naming
	    String name = "Chat";
	    chatImpl = ChatHelper.narrow(ncRef.resolve_str(name));
	    
	    // obtain callback reference for registration w/ server
	    org.omg.CORBA.Object ref = 
		rootpoa.servant_to_reference(chatCallbackImpl);
	    ChatCallback cref = ChatCallbackHelper.narrow(ref);
	    
	    // Application code goes below
		Scanner command = new Scanner(System.in);
	    //chatImpl.say(cref, "\n  Hello stranger...");
		while(true)
		{
			System.out.print("-> ");
			String action = command.next();

			if (action.equals("leave")) 
			{
				chatImpl.leave(cref);
                break;
            }
			else if (action.equals("join")) 
			{
            	chatImpl.join(cref, command.nextLine());
            }
			else if (action.equals("post")) 
			{
            	chatImpl.post(cref, command.nextLine());
            }
			else if (action.equals("list")) 
			{
            	chatImpl.list(cref);
            }
			else if (action.equals("play")) 
			{
				chatImpl.play(cref,command.nextLine());
			}
			else if(action.equals("draw"))
			{
				chatImpl.draw(cref);
			}
			else if(action.equals("put"))
			{
				chatImpl.put(cref, command.nextLine());
			}
			else
			{
				System.out.println("Unknown command");
			}

		}
	    
	} catch(Exception e){
	    System.out.println("ERROR : " + e);
	    e.printStackTrace(System.out);
	}
    }
}
