package modelo;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import control.Error;
import control.IdEstrategia;
import control.dailyOCR;
import control.conexion.ConexionMySql;

public class Estrategia
{	
	protected IdEstrategia id;
	protected List <Senal> senales;
	protected List <Senal> senalesNoSync = new ArrayList <Senal> ();
	private boolean[] activos = new boolean[Par.values().length];
	public Escritor escritor;
	
	public Estrategia()
	{
		senales = Collections.synchronizedList(senalesNoSync);
	}
	
	public Estrategia(IdEstrategia id)
	{
		this.id = id;
		senales = Collections.synchronizedList(senalesNoSync);
	}
	
	public synchronized void cambiarActivo(Par par, boolean activo)
	{
		if(activos == null)
			activos = new boolean[Par.values().length];
		int i = 0;
		for(Par p : Par.values())
		{
			if(p.equals(par))
			{
				activos[i] = activo;
				return;
			}
			i++;
		}
	}
	
	public synchronized boolean darActivo(Par par)
	{
		if(activos == null)
			activos = new boolean[Par.values().length];
		int i = 0;
		for(Par p : Par.values())
		{
			if(p.equals(par))
			{
				return activos[i];
			}
			i++;
		}
		return false;
	}
	
	public synchronized void agregar(SenalEntrada entrada, Senal afectada, boolean dejarLista) 
	{
		if(entrada.getTipo().equals(TipoSenal.HIT))
		{
			hit(entrada, afectada, dejarLista);
		}
		else
		{
			trade(entrada, dejarLista);
		}
	}
	
	protected synchronized void hit(SenalEntrada entrada, Senal afectada, boolean dejarLista) 
	{
		synchronized(senales)
		{
			int lotesAnteriores = afectada.getNumeroLotes();
			escritor.cerrar(entrada, afectada);
			double precioActual = dailyOCR.precioPar(afectada.getPar(), afectada.isCompra());
			double precioParActual = afectada.isCompra() ? precioActual - afectada.getPrecioEntrada() : afectada.getPrecioEntrada() - precioActual;
			int resultado = afectada.getPrecioEntrada() > 10 ? (int) Math.round((precioParActual) * 100) : (int) Math.round((precioParActual) * 10000);
			if(afectada.getNumeroLotes() <= 0)
			{
				if(!dejarLista)
					senales.remove(afectada);
				else
				{
					afectada.setLotesCerradosManualmente(lotesAnteriores);
					afectada.setMagico(new int[1]);
				}
			}
			if(!dejarLista)
			{
				if(afectada.getLotesCerradosManualmente() > 0)
					for(int i = 0; i < afectada.getLotesCerradosManualmente(); i++)
						ConexionMySql.agregarEntrada(id, afectada, System.currentTimeMillis(), resultado);
				else
					for(int i = 0; i < entrada.getNumeroLotes(); i++)
						ConexionMySql.agregarEntrada(id, afectada, System.currentTimeMillis(), resultado);
			}
		}
	}
	
	protected synchronized void trade(SenalEntrada entrada, boolean dejarLista) 
	{
		synchronized(senales)
		{
			Senal nueva = new Senal(id, entrada.isCompra(), entrada.getPar(), entrada.getNumeroLotes(), entrada.getPrecioEntrada());
			nueva.setLimite(entrada.getLimite());
			if(dejarLista)
			{
				nueva.setManual(true);
			}
			if(tienePar(entrada.getPar()) != null)
			{
	    		Error.agregar("Par ya exite en esta estrategia " + id.toString());
	    		return;
			}
			escritor.abrir(entrada, nueva);
			senales.add(nueva);
		}
	}
	
	public synchronized Senal tienePar(Par par) 
	{
		synchronized(senales)
		{
			for(Senal senal : senales)
			{
				if(senal.getPar().equals(par))
					return senal;
			}
		}
		return null;
	}

	public synchronized boolean verificarConsistencia() 
	{
		synchronized(senales)
		{
			return senales == null || id == null || activos == null || escritor == null;
		}
	}
	
    public synchronized void escribir()
    {
    	synchronized(senales)
    	{
	    	try
	    	{
		    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        XMLEncoder encoder = new XMLEncoder(baos);
		        encoder.writeObject(this);
		        encoder.close();
		        String salida = new String(baos.toByteArray());
		        ConexionMySql.guardarPersistencia(id, salida);
	    	}
	    	catch(Exception e)
	    	{
	    		Error.agregar("Error en la escritura en la base de datos: " + id.name());
	    	}
    	}
    }
    
    public static Estrategia leer(IdEstrategia id)
    {
    	try
    	{
	    	String entrada = ConexionMySql.cargarPersistencia(id);
	    	if(entrada.length() < 10)
	    		return null;
	    	char[] entradaChar = entrada.toCharArray();
	    	byte[] entradaByte = new byte[entradaChar.length];
	    	int i = 0;
	    	for(char c : entradaChar)
	    	{
	    		entradaByte[i++] = (byte) c;
	    	}
	    	XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(entradaByte));
	    	Estrategia e = (Estrategia) (decoder.readObject());
	    	decoder.close();
	    	return e;
    	}
    	catch(Exception e)
    	{
    		Error.agregar("Error de lectura de base de datos: " + id.name());
    		return null;
    	}
    }

	public synchronized IdEstrategia getId() {
		return id;
	}

	public synchronized void setId(IdEstrategia id) {
		this.id = id;
	}

	public synchronized List <Senal> getSenales() {
		return senalesNoSync;
	}
	
	public synchronized List <Senal> getSenalesSync() {
		return senales;
	}
	
	public synchronized List <Senal> getSenalesCopy() 
	{
		ArrayList <Senal> senalesNuevas = new ArrayList <Senal> ();
		synchronized(senales)
		{
			if(senales == null)
				return null;
			for(Senal s : senales)
			{
				senalesNuevas.add(s);
			}
		}
		return senalesNuevas;
	}

	public synchronized void setSenales(List <Senal> senales) {
		this.senales = Collections.synchronizedList(senales);
	}

	public synchronized boolean[] getActivos() {
		return activos;
	}

	public synchronized void setActivos(boolean[] activos) {
		this.activos = activos;
	}
}