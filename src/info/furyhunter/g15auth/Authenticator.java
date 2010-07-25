/**
 * G15 Authenticator
 * Copyright (C) 2010 Furyhunter <furyhunter600@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package info.furyhunter.g15auth;

import java.awt.Color;
import java.awt.Graphics2D;

import com.xs0.libs.j15.J15;
import com.xs0.libs.j15.J15ButtonListener;
import com.xs0.libs.j15.J15Connection;
import com.xs0.libs.j15.J15Exception;
import com.xs0.libs.j15.J15Screen;
import authenticator.CodeCalculator;

public class Authenticator implements Runnable {

	public String authcode;
	public byte[] secretkey;
	public long timedifference;
	public int number;
	public J15Connection connection;
	public J15Screen screen;
	public boolean stop;
	public CodeCalculator calc;
	
	public static Authenticator singleton;
	
	/**
	 * Runs the desktop widget
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Requires two arguments: securekey timediff");
			return;
		}
		Authenticator auth = new Authenticator();
		auth.secretkey = hexStringToByteArray(args[0]);
		auth.timedifference = Integer.valueOf(args[1]);
		auth.run();
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	public void generateCode() {
		this.authcode = this.calc.calculateCurrentCode(System.currentTimeMillis());
		
	}
	
	public void drawClock() {
		this.generateCode();
		int w = this.screen.getWidth();
		int h = this.screen.getHeight();
		Graphics2D g = this.screen.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);
		g.setColor(Color.BLACK);
		g.drawString("Battle.net Authenticator", 18, 20);
		g.drawString(this.authcode, 52, 30);
		try {
			this.screen.updateImage();
		} catch (J15Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.err);
		}
	}

	@Override
	public void run() {
		// Set singleton
		singleton = this;
		
		// Generate the code calculator
		this.calc = new CodeCalculator(this.secretkey,this.timedifference);
		
		try {
			// Connect to the G15 daemon
			this.connection = J15.get().connect("Battle.net Authenticator",
					false, false);
		} catch (J15Exception e) {
			System.err.println("Error: Could not connect to G15 daemon");
			e.printStackTrace(System.err);
			return;
		}
		
		// Attach to first screen and call it finished.
		try {
			this.screen = this.connection.attach(0);
			this.screen.addButtonListener(new J15ButtonListener() {
				@Override
				public void onButtonChange(J15Screen source, int newButtonState)
				{
					switch (newButtonState) {
					case 1:
						Authenticator.singleton.drawClock();
						break;
					case 8:
						System.exit(0);
						break;
					}
				}
			});
		} catch (J15Exception e1) {
			System.err.println("Error: Failure to attach to screen");
			e1.printStackTrace(System.err);
			return;
		}
		
		while (!this.stop) {
			// Update the screen
			this.drawClock();
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.err.println("Error: Interrupted Execution");
				e.printStackTrace(System.err);
				break;
			}
		}
	}

}
