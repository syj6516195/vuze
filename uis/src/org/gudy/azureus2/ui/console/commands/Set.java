/*
 * Written and copyright 2001-2003 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * Set.java
 * 
 * Created on 23.03.2004
 *
 */
package org.gudy.azureus2.ui.console.commands;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.gudy.azureus2.ui.common.ExternalUIConst;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * command that allows manipulation of Azureus' runtime properties.
 * - when called without any parameters, it lists all of the available runtime properties.
 * - when called with 1 parameter, it shows the current value of that parameter
 * - when called with 2 or 3 parameters, it assigns a specified value to the 
 *   specified parameter name. (the third parameter forces the property to be set
 *   to a particular type, otherwise we try and guess the type by the current value)
 * @author Tobias Minich, Paul Duran
 */
public class Set extends IConsoleCommand {
	
	private static final String NULL_STRING = "__NULL__";

	public Set()
	{
		super(new String[] {"set", "+" });
	}
	
	public String getCommandDescriptions() {
		return("set [parameter] [value]\t\t+\tSet a configuration parameter. The whitespaceless notation has to be used. If value is omitted, the current setting is shown.");
	}
	
	public void execute(String commandName,ConsoleInput ci, List args) {
		if( args.isEmpty() )
		{
			displayOptions(ci.out);
			return;
		}
		String external_name = (String) args.get(0);
		String internal_name = (String) ExternalUIConst.parameterlegacy.get(external_name);
		if( internal_name == null || internal_name.length() == 0 )
		{
			internal_name = external_name;
		}
//		else
//			ci.out.println("> converting " + origParamName + " to " + parameter);
		
		Parameter param;
		switch( args.size() )
		{
			case 1:
				// try to display the value of the specified parameter
				if( ! COConfigurationManager.doesParameterDefaultExist( internal_name ) )					
				{
					ci.out.println("> Command 'set': Parameter '" + external_name + "' unknown.");
					return;
				}
				param = Parameter.get(internal_name,external_name);
				
				ci.out.println( param.toString() );
				break;
			case 2:
			case 3:
				String setto = (String) args.get(1);
				String type;
				if( args.size() == 2 )
				{
					// guess the parameter type by getting the current value and determining its type
					param = Parameter.get( internal_name, external_name );
					type = param.getType();
				}
				else
					type = (String) args.get(2);
				
				boolean success = false;
				if( type.equalsIgnoreCase("int") ) {
					COConfigurationManager.setParameter( internal_name, Integer.parseInt( setto ) );
					success = true;
				}
				else if( type.equalsIgnoreCase("bool") ) {
					COConfigurationManager.setParameter( internal_name, setto.equalsIgnoreCase("true") ? true : false );
					success = true;
				}
				else if( type.equalsIgnoreCase("float") ) {
					COConfigurationManager.setParameter( internal_name, Float.parseFloat( setto ) );
					success = true;
				}
				else if( type.equalsIgnoreCase("string") ) {
					COConfigurationManager.setParameter( internal_name, setto );
					success = true;
				}
				else if( type.equalsIgnoreCase("password") ) {
					SHA1Hasher hasher = new SHA1Hasher();
					
					byte[] password = setto.getBytes();
					
					byte[] encoded;
					
					if(password.length > 0){
						
						encoded = hasher.calculateHash(password);
						
					}else{
						
						encoded = password;
					}
					
					COConfigurationManager.setParameter( internal_name, encoded );
					
					success = true;
				}
				
				if( success ) {
					COConfigurationManager.save();
					ci.out.println("> Parameter '" + external_name + "' set to '" + setto + "'. [" + type + "]");
				}
				else ci.out.println("ERROR: invalid type given");
				
				break;
			default:
				ci.out.println("Usage: 'set \"parameter\" value type', where type = int, bool, float, string, password");
				break;
		}
	}

	private void displayOptions(PrintStream out)
	{
		Iterator I = COConfigurationManager.getAllowedParameters().iterator();
		Map backmap = new HashMap();
		for (Iterator iter = ExternalUIConst.parameterlegacy.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			backmap.put( entry.getValue(), entry.getKey() );
		}
		TreeSet srt = new TreeSet();
		while (I.hasNext()) {
			String internal_name = (String) I.next();
			
			String	external_name = (String) backmap.get(internal_name);
			
			if ( external_name == null ){
				
				external_name = internal_name;
			}

			System.out.println( "ext=" + external_name + ",int=" + internal_name );
			
			Parameter param = Parameter.get( internal_name, external_name );
			srt.add( param.toString() );
		}
		I = srt.iterator();
		while (I.hasNext()) {
			out.println((String) I.next());
		}
	}

	/**
	 * class that represents a parameter. we can use one of these objects to 
	 * verify a parameter's type and value as well as whether or not a value has been set. 
	 * @author pauld
	 */
	private static class Parameter
	{
		private static final int PARAM_INT = 1;
		private static final int PARAM_BOOLEAN = 2;
		private static final int PARAM_STRING = 4;
		
		/**
		 * returns a new Parameter object reprenting the specified parameter name
		 * @param parameter
		 * @return
		 */
		public static Parameter 
		get(
			String	internal_name,
			String	external_name )
		{
			int underscoreIndex = external_name.indexOf('_');
			int nextchar = external_name.charAt(underscoreIndex + 1);
			try {
				if( nextchar == 'i' )
				{
					int value = COConfigurationManager.getIntParameter(internal_name, Integer.MIN_VALUE);
					return new Parameter(internal_name, external_name, value == Integer.MIN_VALUE ? (Integer)null : new Integer(value) );
				}
				else if( nextchar == 'b' )
				{
					// firstly get it as an integer to make sure it is actually set to something
					if( COConfigurationManager.getIntParameter(internal_name, Integer.MIN_VALUE) != Integer.MIN_VALUE )
					{
						boolean b = COConfigurationManager.getBooleanParameter(internal_name);
						return new Parameter(internal_name, external_name, Boolean.valueOf(b));
					}
					else
					{
						return new Parameter(internal_name, external_name, (Boolean)null);
					}
				}
				else
				{
					String value = COConfigurationManager.getStringParameter(internal_name, NULL_STRING);				
					return new Parameter( internal_name, external_name, NULL_STRING.equals(value) ? null : value);
				}
			} catch (Exception e)
			{
				try {
					int value = COConfigurationManager.getIntParameter(internal_name, Integer.MIN_VALUE);
					return new Parameter(internal_name, external_name, value == Integer.MIN_VALUE ? (Integer)null : new Integer(value) );
				} catch (Exception e1)
				{
					String value = COConfigurationManager.getStringParameter(internal_name);
					return new Parameter( internal_name, external_name, NULL_STRING.equals(value) ? null : value);
				}
			}
		}
		public Parameter( String iname, String ename, Boolean val )
		{
			this(iname,ename, val, PARAM_BOOLEAN);
		}
		public Parameter( String iname, String ename, Integer val )
		{
			this(iname,ename, val, PARAM_INT);
		}
		public Parameter( String iname, String ename, String val )
		{
			this(iname,ename, val, PARAM_STRING);
		}
		private Parameter( String _iname, String _ename, Object _val, int _type )
		{
			type = _type;
			iname = _iname;
			ename = _ename;
			value = _val;
			isSet = (value != null);
			
			if ( !isSet ){
				
				def = COConfigurationManager.getDefault(iname);
				
				if  ( def != null ){
					
					if ( def instanceof Long ){
						
						type = PARAM_INT;
					}
				}
			}
		}
		private int type;
		private String iname;
		private String ename;
		private Object value;
		private boolean isSet;
		private Object	def;
		
		public String getType()
		{
			switch( type )
			{
				case PARAM_BOOLEAN:
					return "bool";
				case PARAM_INT:
					return "int";
				case PARAM_STRING:
					return "string";
				default:
					return "unknown";
			}
		}	
		public String toString()
		{
			if( isSet ){
				return "> " + ename + ": " + value + " [" + getType() + "]";				
			}else{
				if ( def == null ){
					
					return "> " + ename + " is not set. [" + getType() + "]";
					
				}else{
					return "> " + ename + " is not set. [" + getType() + ", default: " + def + "]";
				}
			}
		}
	}
}
