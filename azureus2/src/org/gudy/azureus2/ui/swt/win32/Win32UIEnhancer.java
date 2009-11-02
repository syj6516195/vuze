/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.win32;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.platform.win32.access.AEWin32Manager;

import com.aelitis.azureus.core.drivedetector.*;

/**
 * @author TuxPaper
 * @created Nov 29, 2006
 *
 * Note: You can safely exclude this class from the build path.
 * All calls to this class use (or at least should use) reflection
 */
public class Win32UIEnhancer
{

	public static final boolean DEBUG = false;

	public static final int SHGFI_LARGEICON = 0x2;

	public static final int WM_DEVICECHANGE = 0x219;

	public static final int DBT_DEVICEARRIVAL = 0x8000;

	public static final int DBT_DEVICEREMOVECOMPLETE = 0x8004;

	public static final int DBT_DEVTYP_VOLUME = 0x2;

	private static int messageProcInt;

	private static long messageProcLong;

	private static Object /* Callback */messageCallback;

	private static DriveDetectedInfo loc;

	private static Class<?> claOS;

	private static boolean useLong;

	private static Class<?> claCallback;

	private static Constructor<?> constCallBack;

	private static Method mCallback_getAddress;

	private static Method mSetWindowLongPtr;

	private static int OS_GWLP_WNDPROC;

	private static Method mOS_memmove_byte;

	private static Method mOS_memmove_int;

	static {
		try {
			claOS = Class.forName("org.eclipse.swt.internal.win32.OS");

			// public Callback (Object object, String method, int argCount)
			claCallback = Class.forName("org.eclipse.swt.internal.Callback");
			constCallBack = claCallback.getDeclaredConstructor(new Class[] {
				Object.class,
				String.class,
				int.class
			});
			// public long /*int*/ getAddress ()
			mCallback_getAddress = claCallback.getDeclaredMethod("getAddress",
					new Class[] {});

			try {
				//int /*long*/ SetWindowLongPtr (int /*long*/ hWnd, int nIndex, int /*long*/ dwNewLong) {
				mSetWindowLongPtr = claOS.getMethod("SetWindowLongPtr",
						new Class[] {
							int.class,
							int.class,
							int.class
						});

				useLong = false;
				
				mOS_memmove_byte = claOS.getMethod("memmove", new Class[] {
					byte[].class,
					int.class,
					int.class
				});
				mOS_memmove_int = claOS.getMethod("memmove", new Class[] {
					int[].class,
					int.class,
					int.class
				});
			} catch (Exception e) {
				e.printStackTrace();
				mSetWindowLongPtr = claOS.getMethod("SetWindowLongPtr",
						new Class[] {
							long.class,
							int.class,
							long.class
						});

				useLong = true;
				mOS_memmove_byte = claOS.getMethod("memmove", new Class[] {
					byte[].class,
					long.class,
					int.class
				});
				mOS_memmove_int = claOS.getMethod("memmove", new Class[] {
					int[].class,
					int.class,
					long.class
				});
			}

			//OS.GWLP_WNDPROC
			OS_GWLP_WNDPROC = ((Integer) claOS.getField("GWLP_WNDPROC").get(null)).intValue();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static Image getFileIcon(File file, boolean big) {
		int flags = OS.SHGFI_ICON;
		flags |= big ? SHGFI_LARGEICON : OS.SHGFI_SMALLICON;
		if (!file.exists()) {
			flags |= OS.SHGFI_USEFILEATTRIBUTES;
		}
		SHFILEINFO shfi = OS.IsUnicode ? (SHFILEINFO) new SHFILEINFOW()
				: new SHFILEINFOA();
		TCHAR pszPath = new TCHAR(0, file.getAbsolutePath(), true);
		OS.SHGetFileInfo(pszPath, file.isDirectory() ? 16
				: OS.FILE_ATTRIBUTE_NORMAL, shfi, SHFILEINFO.sizeof, flags);
		if (shfi.hIcon != 0) {
			Image image = Image.win32_new(null, SWT.ICON, shfi.hIcon);
			return image;
		}

		return null;
	}

	public static void initMainShell(Shell shell) {
		//Canvas canvas = new Canvas(shell, SWT.NO_BACKGROUND | SWT.NO_TRIM);
		//canvas.setVisible(false);
		Shell subshell = new Shell(shell);

		try {
			messageCallback = constCallBack.newInstance(new Object[] {
				Win32UIEnhancer.class,
				"messageProc2",
				4
			});

			if (useLong) {
				Number n = (Number) mCallback_getAddress.invoke(messageCallback,
						new Object[] {});
				messageProcLong = n.longValue();
				if (messageProcLong != 0) {
					mSetWindowLongPtr.invoke(null, new Object[] {
						subshell.handle,
						OS_GWLP_WNDPROC,
						messageProcLong
					});
				}
			} else {
				Number n = (Number) mCallback_getAddress.invoke(messageCallback,
						new Object[] {});
				messageProcInt = n.intValue();
				if (messageProcInt != 0) {
					mSetWindowLongPtr.invoke(null, new Object[] {
						subshell.handle,
						OS_GWLP_WNDPROC,
						messageProcInt
					});
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		File[] drives = AEWin32Manager.getAccessor(false).getUSBDrives();
		if (drives != null) {
			for (File file : drives) {
				DriveDetectorFactory.getDeviceDetector().driveDetected(file);
			}
		}
	}

	static int /*long*/messageProc2(int /*long*/hwnd, int /*long*/msg,
			int /*long*/wParam, int /*long*/lParam) {
		return (int) messageProc2(hwnd, msg, (long) wParam, (long) lParam);
	}

	static long /*int*/messageProc2(long /*int*/hwnd, long /*int*/msg,
			long /*int*/wParam, long /*int*/lParam) {
		try {
			// I'll clean this up soon
			switch ((int) /*64*/msg) {
				case WM_DEVICECHANGE:
					if (wParam == DBT_DEVICEARRIVAL) {
						int[] st = new int[3];
						if (useLong) {
  						mOS_memmove_int.invoke(null, new Object[] {
  							st,
  							lParam,
  							(long) 12
  						});
						} else {
  						mOS_memmove_int.invoke(null, new Object[] {
  							st,
  							(int) lParam,
  							(int) 12
  						});
						}

						if (DEBUG) {
							System.out.println("Arrival: " + st[0] + "/" + st[1] + "/"
									+ st[2]);
						}

						if (st[1] == DBT_DEVTYP_VOLUME) {
							if (DEBUG) {
								System.out.println("NEW VOLUME!");
							}

							byte b[] = new byte[st[0]];
							
							if (useLong) {
  							mOS_memmove_byte.invoke(null, new Object[] {
  								b,
  								lParam,
  								(int) st[0]
  							});
							} else {
  							mOS_memmove_byte.invoke(null, new Object[] {
  								b,
  								(int) lParam,
  								(int) st[0]
  							});
							}
							long unitMask = b[12] + (b[13] << 8) + (b[14] << 16)
									+ (b[14] << 24);
							char letter = '?';
							for (int i = 0; i < 26; i++) {
								if (1 << i == unitMask) {
									letter = (char) ('A' + i);
								}
							}
							if (DEBUG) {
								System.out.println("Drive " + letter);
							}
							if (letter != '?') {
								DriveDetector driveDetector = DriveDetectorFactory.getDeviceDetector();
								driveDetector.driveDetected(new File(letter + ":\\"));
							}
						}

					} else if (wParam == DBT_DEVICEREMOVECOMPLETE) {
						int[] st = new int[3];
						if (useLong) {
  						mOS_memmove_int.invoke(null, new Object[] {
  							st,
  							lParam,
  							(long) 12
  						});
						} else {
  						mOS_memmove_int.invoke(null, new Object[] {
  							st,
  							(int) lParam,
  							(int) 12
  						});
						}

						if (DEBUG) {
							System.out.println("Remove: " + st[0] + "/" + st[1] + "/" + st[2]);
						}

						if (st[1] == DBT_DEVTYP_VOLUME) {
							if (DEBUG) {
								System.out.println("REMOVE VOLUME!");
							}

							byte b[] = new byte[st[0]];
							if (useLong) {
  							mOS_memmove_byte.invoke(null, new Object[] {
  								b,
  								lParam,
  								(int) st[0]
  							});
							} else {
  							mOS_memmove_byte.invoke(null, new Object[] {
  								b,
  								(int) lParam,
  								(int) st[0]
  							});
							}
							long unitMask = b[12] + (b[13] << 8) + (b[14] << 16)
									+ (b[14] << 24);
							char letter = '?';
							for (int i = 0; i < 26; i++) {
								if (1 << i == unitMask) {
									letter = (char) ('A' + i);
								}
							}
							if (DEBUG) {
								System.out.println("Drive " + letter);
							}
							if (letter != '?') {
								DriveDetector driveDetector = DriveDetectorFactory.getDeviceDetector();
								driveDetector.driveRemoved(new File(letter + ":\\"));
							}
						}

					}
					if (DEBUG) {
						System.out.println("DEVICE CHANGE" + wParam + "/" + lParam);
					}
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;// OS.DefWindowProc (hwnd, (int)/*64*/msg, wParam, lParam);
	}
}
