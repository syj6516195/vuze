/*
 * Created on 7 Jun 2007
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.core.peermanager.utils;

/**
 * @author Allan Crooks
 */
class BTPeerIDByteDecoderUtils {
	
	public static String decodeMnemonic(char c) {
		switch (c) {
			case 'b':
			case 'B':
				return "Beta";
		}
		return null;
	}
		
	public static String decodeNumericValueOfByte(byte b) {
		return String.valueOf((int)b & 0xFF);
	}
	
	public static String decodeNumericValueOfByte(byte b, int min_digits) {
		String result = decodeNumericValueOfByte(b);
		while (result.length() < min_digits) {result = "0" + result;}
		return result;
	}
	
	public static String decodeNumericChar(char c) {
		String result = decodeAlphaNumericChar(c);
		if (result == null || result.length() == 1) {return result;}
		return null;
	}
	
	public static String intchar(char c) {
		String result = decodeNumericChar(c);
		if (result == null) {throw new IllegalArgumentException("not an integer character: " + c);}
		return result;
	}

	public static String decodeAlphaNumericChar(char c) {
		switch (c) {
			case '0':
				return "0";
			case '1':
				return "1";
			case '2':
				return "2";
			case '3':
				return "3";
			case '4':
				return "4";
			case '5':
				return "5";
			case '6':
				return "6";
			case '7':
				return "7";
			case '8':
				return "8";
			case '9':
				return "9";
			case 'A':
				return "10";
			case 'B':
				return "11";
			case 'C':
				return "12";
			case 'D':
				return "13";
			case 'E':
				return "14";
			case 'F':
				return "15";
			case 'G':
				return "16";
			case 'H':
				return "17";
			case 'I':
				return "18";
			case 'J':
				return "19";
			case 'K':
				return "20";
			case 'L':
				return "21";
			case 'M':
				return "22";
			case 'N':
				return "23";
			case 'O':
				return "24";
			case 'P':
				return "25";
			case 'Q':
				return "26";
			case 'R':
				return "27";
			case 'S':
				return "28";
			case 'T':
				return "29";
			case 'U':
				return "30";
			case 'V':
				return "31";
			case 'W':
				return "32";
			case 'X':
				return "33";
			case 'Y':
				return "34";
			case 'Z':
				return "35";
			case 'a':
				return "36";
			case 'b':
				return "37";
			case 'c':
				return "38";
			case 'd':
				return "39";
			case 'e':
				return "40";
			case 'f':
				return "41";
			case 'g':
				return "42";
			case 'h':
				return "43";
			case 'i':
				return "44";
			case 'j':
				return "45";
			case 'k':
				return "46";
			case 'l':
				return "47";
			case 'm':
				return "48";
			case 'n':
				return "49";
			case 'o':
				return "50";
			case 'p':
				return "51";
			case 'q':
				return "52";
			case 'r':
				return "53";
			case 's':
				return "54";
			case 't':
				return "55";
			case 'u':
				return "56";
			case 'v':
				return "57";
			case 'w':
				return "58";
			case 'x':
				return "59";
			case 'y':
				return "60";
			case 'z':
				return "61";
			case '.':
				return "62";
			case '-':
				return "63";
		}
		return null;
	}
	
	public static boolean isAzStyle(String peer_id) {
		if (peer_id.charAt(0) != '-') {return false;}
		if (peer_id.charAt(7) == '-') {return true;}
		
		// Hack for FlashGet - it doesn't use the trailing
		return peer_id.substring(1, 3).equals("FG");
	}
	
	public static boolean isShadowStyle(String peer_id) {
		if (peer_id.charAt(5) != '-') {return false;}
		if (peer_id.charAt(6) != '-') {return false;}
		if (peer_id.charAt(7) == '-') {return true;}
		
		// Hack for ABC, it only uses two trailing dashes
		// rather than 3.
		return peer_id.charAt(0) == 'A';
	}
	
	public static boolean isMainlineStyle(String peer_id) {
		/**
		 * One of the following styles will be used:
		 *   Mx-y-z--
		 *   Mx-yy-z-
		 */ 
		return peer_id.charAt(2) == '-' && peer_id.charAt(7) == '-' && (
				peer_id.charAt(4) == '-' || peer_id.charAt(5) == '-');
	}
	
	public static boolean isPossibleSpoofClient(String peer_id) {
		return peer_id.endsWith("UDP0") || peer_id.endsWith("HTTPBT");
	}
	
	public static String getMainlineStyleVersionNumber(String peer_id) {
		boolean two_digit_in_middle = peer_id.charAt(5) == '-';
		String middle_part = decodeNumericChar(peer_id.charAt(3));
		if (two_digit_in_middle) {
			middle_part = join(middle_part, decodeNumericChar(peer_id.charAt(4)));
		}
		return joinAsDotted(
			decodeNumericChar(peer_id.charAt(1)), middle_part,
			decodeNumericChar(peer_id.charAt(two_digit_in_middle ? 6 : 5))
		);
	}
	
	public static String getShadowStyleVersionNumber(String peer_id) {
		String ver_number = decodeAlphaNumericChar(peer_id.charAt(1));
		if (ver_number == null) {return null;}
		for (int i=2; i<6 && ver_number != null; i++) {
			char c = peer_id.charAt(i);
			if (c == '-') {break;}
			ver_number = joinAsDotted(ver_number, decodeAlphaNumericChar(peer_id.charAt(i)));
		}
		// We'll strip off trailing redundant zeroes - BitTornado seems to have the 0
		// present in the peer ID, but doesn't mention the fourth digit on their site..
		while (ver_number.endsWith(".0")) {ver_number = ver_number.substring(0, ver_number.length()-2);}
		return ver_number;
	}
	
	public static String decodeAzStyleVersionNumber(String version_data, String version_scheme) {
		char a = version_data.charAt(0);
		char b = version_data.charAt(1);
		char c = version_data.charAt(2);
		char d = version_data.charAt(3);
		
		if (version_scheme == BTPeerIDByteDecoderDefinitions.VER_AZ_FOUR_DIGITS) {
			return intchar(a) + "." + intchar(b) + "." + intchar(c) + "." + intchar(d);
		}
		else if (version_scheme == BTPeerIDByteDecoderDefinitions.VER_AZ_THREE_DIGITS ||
				version_scheme == BTPeerIDByteDecoderDefinitions.VER_AZ_THREE_DIGITS_PLUS_MNEMONIC) {
			String result = intchar(a) + "." + intchar(b) + "." + intchar(c);
			if (version_scheme == BTPeerIDByteDecoderDefinitions.VER_AZ_THREE_DIGITS_PLUS_MNEMONIC) {
				String mnemonic = decodeMnemonic(d);
				if (mnemonic != null) {result += " " + mnemonic;}
			}
			return result;
		}
		else if (version_scheme == BTPeerIDByteDecoderDefinitions.VER_AZ_TWO_MAJ_TWO_MIN) {
			return (a == '0' ? "" : intchar(a)) + intchar(b) + "." + intchar(c) + intchar(d);
		}
		else if (version_scheme == BTPeerIDByteDecoderDefinitions.VER_AZ_LAST_THREE_DIGITS) {
			return intchar(b) + "." + intchar(c) + intchar(d);
		}
		else if (version_scheme == BTPeerIDByteDecoderDefinitions.VER_AZ_THREE_ALPHANUMERIC_DIGITS) {
			return decodeAlphaNumericChar(a) + "." + decodeAlphaNumericChar(b) + "." + decodeAlphaNumericChar(c);
		}
		else if (version_scheme == BTPeerIDByteDecoderDefinitions.VER_AZ_KTORRENT_STYLE) {
			// Either something like this:
			//   1.2 RC 4 [where 3 == 'R')
			//   1.2 Dev  [where 3 == 'D')
			//   1.2      [where 3 doesn't equal the above]
			switch (c) {
				case 'R':
					return intchar(a) + "." + intchar(b) + " RC" + intchar(d);
				case 'D':
					return intchar(a) + "." + intchar(b) + " Dev";
				default:
					return intchar(a) + "." + intchar(b);
			}
		}
		else if (version_scheme.equals("1.234")) {
			return intchar(a) + "." + intchar(b) + intchar(c) + intchar(d);
		}
		else if (version_scheme.equals("1.2(34)")) {
			return intchar(a) + "." + intchar(b) + "(" + intchar(c) + intchar(d) + ")"; 
		}
		else if (version_scheme.equals("1.2.34")) {
			return intchar(a) + "." + intchar(b) + "." + intchar(c) + intchar(d);
		}
		else if (version_scheme.equals("v1234")) {
			return "v" + intchar(a) + intchar(b) + intchar(c) + intchar(d);
		}
		else {
			throw new RuntimeException("unknown AZ style version number scheme - " + version_scheme);
		}
	}
	
	public static String getTwoByteThreePartVersion(byte b1, byte b2) {
		String min_part = decodeNumericValueOfByte(b2, 2);
		return joinAsDotted(decodeNumericValueOfByte(b1), min_part.substring(0, 1), min_part.substring(1, 2));
	}
	
	public static String decodeCustomVersionNumber(String version_data, String version_scheme) {
		if (version_scheme == BTPeerIDByteDecoderDefinitions.VER_BLOCK) {
			return version_data;
		}
		else if (version_scheme == BTPeerIDByteDecoderDefinitions.VER_DOTTED_BLOCK ||
				version_scheme == BTPeerIDByteDecoderDefinitions.VER_BYTE_BLOCK_DOTTED_CHAR) {
			int inc_size = (version_scheme == BTPeerIDByteDecoderDefinitions.VER_DOTTED_BLOCK) ? 2 : 1;
			String result = version_data.substring(0, 1);
			for (int i=0+inc_size; i<version_data.length(); i+=inc_size) {
				result = joinAsDotted(result, String.valueOf(version_data.charAt(i)));
			}
			return result;
		}
		else if (version_scheme == BTPeerIDByteDecoderDefinitions.VER_BITS_ON_WHEELS) {
			if (version_data.equals("A0C")) {return "1.0.6";}
			else if (version_data.equals("A0B")) {return "1.0.5";}
			throw new RuntimeException("Unknown BitsOnWheels version number - " + version_data);
		}
		else {
			throw new RuntimeException("unknown custom version number scheme - " + version_scheme);
		}
	}
	
	private static String join(String a, String b) {
		if (a == null) {return null;}
		if (b == null) {return null;}
		return a + b;
	}
	
	private static String joinAsDotted(String a, String b) {
		if (a == null) {return null;}
		if (b == null) {return null;}
		return a + "." + b;
	}

	
	private static String joinAsDotted(String a, String b, String c) {
		if (a == null) {return null;}
		if (b == null) {return null;}
		if (c == null) {return null;}
		return a + "." + b + "." + c;
	}
	
	
	
}
