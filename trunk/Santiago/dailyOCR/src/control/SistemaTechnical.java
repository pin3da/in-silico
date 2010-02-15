package control;
import java.io.File;

import java.util.ArrayList;


import servidor.ConexionServidor;



public class SistemaTechnical extends SistemaEstrategias 
{
	Estrategia technical;
	File t = new File(pathPersistencia + "technical.o");
	
	public void cargarEstrategias() 
	{
		if(t.exists())
		{
			technical = Estrategia.leer(t);
		}
		else
		{
			technical = new Estrategia(IdEstrategia.TECHNICAL);
		}
		technical.escritor = escritor;
		try
		{
			metodoLectura = ConexionServidor.class.getMethod("leerServidorTechnical");
		}
		catch (Exception e)
		{
    		Error.agregar(e.getMessage() + " Error metodo invalido en Sistema technical");
		}
	}

	public void verificarConsistencia() 
	{
		if(technical == null)
		{
			cargarEstrategias();
		}
	}
	
	protected ArrayList <Senal> leer(String [] contenidos)
	{
		ArrayList <Senal> nuevas = new ArrayList <Senal> (10);
		for(int i = 0; i < contenidos.length; i++)
		{
			String actual = contenidos[i];
			String par = actual.substring(0, actual.indexOf(" "));
			Par estePar = Par.convertirPar(par);
			int indice = actual.indexOf("Our preference:");
			if(indice == -1)
			{
				Error.agregar("Mensaje invalido: " + actual);
				continue;
			}
			actual = actual.substring(indice);
			boolean compra = actual.contains("Long") || actual.contains("long") || actual.contains("buy") || actual.contains("Buy");
			ArrayList <Double> valores = new ArrayList <Double> (10);
			String [] partido = actual.split(" ");
			for(String s : partido)
			{
				try
				{
					double prueba = Double.parseDouble(s);
					valores.add(prueba);
				}
				catch(Exception e)
				{
				}
			}
			try
			{
				double limite = valores.get(2);
				nuevas.add(new Senal(IdEstrategia.TECHNICAL, compra, estePar, 1, limite));
			}
			catch(Exception e)
			{
	    		Error.agregar(e.getMessage() + " Error al a�adir nuevas estrategias en SIstema technical");
			}
		}
		return nuevas;
	}
	
	protected void procesar(ArrayList <Senal> senalesLeidas)
	{
		for(Senal senal : senalesLeidas)
		{
			boolean esta = false;
			for(Senal otra : technical.senales)
			{
				if(senal.par == otra.par)
				{
					esta = true;
					if(senal.compra != otra.compra)
					{
						technical.agregar(new SenalEntrada(senal.par, TipoSenal.HIT, false, 1, 0), otra, false);
						break;
					}
				}
			}
			if(!esta)
			{
		    	SenalEntrada nueva = new SenalEntrada(senal.par, TipoSenal.TRADE, senal.compra, 1, dailyOCR.precioPar(senal.par, senal.compra));
		    	nueva.limite = senal.precioEntrada;
		    	if(nueva.limite > 10)
		    	{
		    		double anteriorLimite;
		    		if(senal.compra)
		    			anteriorLimite = senal.precioEntrada - 0.8;
		    		else
		    			anteriorLimite = senal.precioEntrada + 0.8;
		    		if(senal.compra && dailyOCR.precioPar(senal.par, senal.compra) > anteriorLimite)
		    			break;
		    		if(!senal.compra && dailyOCR.precioPar(senal.par, senal.compra) < anteriorLimite)
		    			break;
		    	}
		    	else
		    	{
		    		double anteriorLimite;
		    		if(senal.compra)
		    			anteriorLimite = senal.precioEntrada - 0.008;
		    		else
		    			anteriorLimite = senal.precioEntrada + 0.008;
		    		if(senal.compra && dailyOCR.precioPar(senal.par, senal.compra) > anteriorLimite)
		    			break;
		    		if(!senal.compra && dailyOCR.precioPar(senal.par, senal.compra) < anteriorLimite)
		    			break;
		    	}
				technical.agregar(nueva, senal, false);
			}
		}
		for(Senal otra : technical.senales)
		{
			double otroPrecio = dailyOCR.precioPar(otra.par, !otra.compra);
			if(otra.compra && otra.limite < otroPrecio)
			{
				technical.agregar(new SenalEntrada(otra.par, TipoSenal.HIT, false, 1, 0), otra, false);
			}
			if(!otra.compra && otra.limite > otroPrecio)
			{
				technical.agregar(new SenalEntrada(otra.par, TipoSenal.HIT, false, 1, 0), otra, false);
			}
		}
	}
	
	public void persistir() 
	{
		technical.escribir(t);
	}
	
	public Estrategia darEstrategia(IdEstrategia id)
	{
		if(id == IdEstrategia.TECHNICAL)
		{
			return technical;
		}
		return null;
	}
}