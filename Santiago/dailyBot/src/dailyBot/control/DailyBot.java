package dailyBot.control;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import dailyBot.control.conexion.ConexionRMI;
import dailyBot.control.conexion.ConexionServidorRMI;
import dailyBot.control.conexion.dailyFx.ConexionServidorDailyFx;
import dailyBot.modelo.Par;
import dailyBot.modelo.SistemaEstrategias;
import dailyBot.modelo.Estrategia.IdEstrategia;
import dailyBot.modelo.Proveedor.IdProveedor;
import dailyBot.modelo.dailyFx.SistemaDailyFX;
import dailyBot.vista.VentanaPrincipal;

public class DailyBot
{
	private static ArrayList <SistemaEstrategias> sistemas;
	private static Class <?> [] clasesSistemas = {
										    SistemaDailyFX.class
											//SistemaJoel.class,
										  //SistemaTechnical.class
	};
	
	private static void cargarSistemasEstrategias()
	{
		sistemas = new ArrayList <SistemaEstrategias> ();
		for(Class <?> clase : clasesSistemas)
		{
			try 
			{
				sistemas.add((SistemaEstrategias) (clase.getConstructor(new Class <?> [0]).newInstance(new Object[0])));
			} 
			catch (Exception e)
			{
				Error.agregar("Error inicializando la clase: " + clase.getCanonicalName());
			}
		}
	}
	
	private static void cargarEstrategias()
	{
		for(IdEstrategia e : IdEstrategia.values())
			e.iniciarEstrategia();
	}
	
	private static void cargarProveedores()
	{
		for(IdProveedor p : IdProveedor.values())
			p.iniciarProveedor();
	}

	private static void iniciarHilos()
	{
		HiloDaily hiloSSIVix = new HiloDaily(new RunnableDaily()
		{
			public void run() 
			{
				while(true)
				{
					boolean diezYMedia = false;
					boolean diezYNueveYMedia = false;
					try
					{
						Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						int hora = calendar.get(Calendar.HOUR_OF_DAY);
						int minuto = calendar.get(Calendar.MINUTE);
						int dia = calendar.get(Calendar.DAY_OF_WEEK);
						if((hora == 8 && minuto >= 30) || (hora > 9 && hora < 17))
						{
							diezYNueveYMedia = false;
							if(!diezYMedia)
								diezYMedia = ConexionServidorDailyFx.cargarSSI();
						}
						else
						{
							diezYMedia = false;
							if(!diezYNueveYMedia)
								 diezYNueveYMedia = ConexionServidorDailyFx.cargarSSI();
						}
						ConexionServidorDailyFx.cargarVIX();
						if(dia == Calendar.FRIDAY && hora > 21)
						{
							try
							{
								Runtime.getRuntime().exec("/home/santiago/backup");
								HiloDaily.sleep(60000);
								Calendar actual = Calendar.getInstance();
								Error.agregarInfo("Apagando equipo automaticamente: " + actual.get(Calendar.DAY_OF_MONTH) + "/" + (actual.get(Calendar.MONTH) + 1) + "/" + actual.get(Calendar.YEAR) + " " + actual.get(Calendar.HOUR_OF_DAY) + ":" + actual.get(Calendar.MINUTE) + ":" + actual.get(Calendar.SECOND) + "." + actual.get(Calendar.MILLISECOND));
								HiloDaily.sleep(600000);
								for(Par p : Par.values())
									if(p != Par.TODOS)
										p.cerrarDia();
								Runtime.getRuntime().exec("shutdown now -P");
								System.exit(0);
							}
							catch(Exception e)
							{
								System.exit(0);
							}
						}
						HiloDaily.sleep(600000);
						
					}
					catch(Exception e)
					{
						Error.agregar("Error en el hilo monitor de ConexionServidor");
					}
					ponerUltimaActulizacion(System.currentTimeMillis());
				}
			}
		}, 1200000);
		hiloSSIVix.setName("Monitor VIX-SSI");
		AdministradorHilos.agregarHilo(hiloSSIVix);
		HiloDaily hiloPares = new HiloDaily(new RunnableDaily()
		{
			public void run() 
			{
				HiloDaily.sleep(180000);
				while(true)
				{
					try
					{
						HiloDaily.sleep(30000);
						for(Par p : Par.values())
							p.procesarSenales();
					}
					catch(Exception e)
					{
						Error.agregar("Error en el monitor de pares " + e.getMessage());
					}
					ponerUltimaActulizacion(System.currentTimeMillis());
				}
				
			}
		}, 360000L);
		hiloPares.setName("Monitor pares");
		AdministradorHilos.agregarHilo(hiloPares);
		for(SistemaEstrategias sistema : sistemas)
			sistema.iniciarHilo();
	}
	
	public static void salir()
	{
		if(sistemas != null)
			salir(0);
		else
			System.exit(0);
	}
	
	private static void salir(int n)
	{
		if(n == sistemas.size())
		{
			for(IdProveedor p : IdProveedor.values())
			{
				p.darProveedor().escribir();
				p.darProveedor().cerrar();
			}
			for(IdEstrategia e : IdEstrategia.values())
				e.darEstrategia().escribir();
			System.exit(0);
		}
		else
		{
			sistemas.get(n).lockSistema();
			sistemas.get(n).persistir();
			salir(n + 1);
		}	
	}
	
	public static void iniciarPropiedades()
	{
		System.setProperty("java.rmi.server.codebase", "file:" + System.getProperty("user.dir") + "/bin/");
		System.setProperty("java.security.policy", "file:" + System.getProperty("user.dir") + "/libs/server.policy");
	}
	
	public static void main(String [] args) throws IOException
	{
		Calendar actual = Calendar.getInstance();
		Error.agregarInfo("Iniciando operaciones automaticamente: " + actual.get(Calendar.DAY_OF_MONTH) + "/" + (actual.get(Calendar.MONTH) + 1) + "/" + actual.get(Calendar.YEAR) + " " + actual.get(Calendar.HOUR_OF_DAY) + ":" + actual.get(Calendar.MINUTE) + ":" + actual.get(Calendar.SECOND) + "." + actual.get(Calendar.MILLISECOND));
		iniciarPropiedades();
		System.setSecurityManager(new SecurityManager());
        try 
        {
            String name = "Conexion";
            ConexionServidorRMI conexion = new ConexionServidorRMI();
            ConexionRMI stub = (ConexionRMI) UnicastRemoteObject.exportObject(conexion, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(name, stub);
        } 
        catch (Exception e)
        {
        	Error.agregar(e.getMessage() + " Error haciendo la conexion RMI");
        	Error.reiniciarSinPersistir();
        }
		cargarSistemasEstrategias();
		cargarEstrategias();
		cargarProveedores();
		iniciarHilos();
		VentanaPrincipal.iniciar(true);
	}
}
