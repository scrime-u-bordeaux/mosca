/*
Mosca: SuperCollider class by Iain Mott, 2016. Licensed under a
Creative Commons Attribution-NonCommercial 4.0 International License
http://creativecommons.org/licenses/by-nc/4.0/
The class makes extensive use of the Ambisonic Toolkit (http://www.ambisonictoolkit.net/)
by Joseph Anderson and the Automation quark
(https://github.com/neeels/Automation) by Neels Hofmeyr.
Required Quarks : Automation, Ctk, XML and  MathLib
Required classes:
SC Plugins: https://github.com/supercollider/sc3-plugins
User must set up a project directory with subdirectoties "rir" and "auto"
RIRs should have the first 100 or 120ms silenced to act as "tail" reverberators
and must be placed in the "rir" directory.
Run help on the "Mosca" class in SuperCollider for detailed information
and code examples. Further information and sample RIRs and B-format recordings
may be downloaded here: http://escuta.org/mosca
*/

+ Mosca {

	headTracker { | serport, offsetheading = 0 |

		hdtrk = true;
		ioffsetheading = offsetheading;
		SerialPort.devicePattern = serport;
		// needed in serKeepItUp routine - see below
		trackPort = SerialPort(serport, 115200, crtscts: true);
		//trackarr= [251, 252, 253, 254, nil, nil, nil, nil, nil, nil,
		//	nil, nil, nil, nil, nil, nil, nil, nil, 255];  //protocol
		trackarr= [251, 252, 253, 254, nil, nil, nil, nil, nil, nil, 255];
		//protocol
		trackarr2= trackarr.copy;
		tracki= 0;
		//track2arr=
		//[247, 248, 249, 250, nil, nil, nil, nil, nil, nil, nil, nil, 255];
		//protocol
		//track2arr2= trackarr.copy;
		//track2i= 0;


		trackPort.doneAction = {
			"Serial port down".postln;
			troutine.stop;
			troutine.reset;
		};


		troutine = Routine.new({
			inf.do{
				this.matchTByte(trackPort.read);
			};
		});

		kroutine = Routine.new({
			inf.do{
				if (trackPort.isOpen.not) // if serial port is closed
				{
					"Trying to reopen serial port!".postln;
					if (SerialPort.devices.includesEqual(serport))
					// and if device is actually connected
					{
						"Device connected! Opening port!".postln;
						troutine.stop;
						troutine.reset;
						trackPort = SerialPort(serport, 115200,
							crtscts: true);
						troutine.play; // start tracker routine again
					}
				};
				1.wait;
			};
		});

		//troutine = this.trackerRoutine; // start parsing of serial head tracker data
		//	kroutine = this.serialKeepItUp;
		troutine.play;
		kroutine.play;
	}

		//	procTracker  {|heading, roll, pitch, lat, lon|
	procTracker  {|heading, roll, pitch|
		var h, r, p;
		//lattemp, lontemp, newOX, newOY;
		h = (heading / 100) - pi;
		h = h - ioffsetheading;
		if (h < -pi) {
			h = pi + (pi + h);
		};
		if (h > pi) {
			h = -pi - (pi - h);
		};

		r = (roll / 100) - pi;
		p = (pitch / 100) - pi;

		// Arduino code needs changing?

		pitchnumboxProxy.valueAction = p * -1;
		rollnumboxProxy.valueAction = r;
		headingnumboxProxy.valueAction = h * -1;
	}

	matchTByte { |byte|  // match incoming headtracker data

		if(trackarr[tracki].isNil or:{trackarr[tracki]==byte}, {
			trackarr2[tracki]= byte;
			tracki= tracki+1;
			if(tracki>=trackarr.size, {
				//				this.procTracker(trackarr2[4]<<8+trackarr2[5],
				//				trackarr2[6]<<8+trackarr2[7],
				//              trackarr2[8]<<8+trackarr2[9],
				if(hdtrk){
					this.procTracker(
						(trackarr2[5]<<8)+trackarr2[4],
						(trackarr2[7]<<8)+trackarr2[6],
						(trackarr2[9]<<8)+trackarr2[8]
						//,
						//(trackarr2[13]<<24) + (trackarr2[12]<<16) +
						//(trackarr2[11]<<8)
						//+ trackarr2[10],
						//(trackarr2[17]<<24) + (trackarr2[16]<<16) +
						//(trackarr2[15]<<8)
						//+ trackarr2[14]
					);
				};
				tracki= 0;
			});
		}, {
			tracki= 0;
		});
	}

	trackerRoutine { Routine.new
		( {
			inf.do{
				//trackPort.read.postln;
				this.matchTByte(trackPort.read);
			};
		})
	}

	serialKeepItUp {Routine.new({
		inf.do{
			if (trackPort.isOpen.not) // if serial port is closed
			{
				"Trying to reopen serial port!".postln;
				if (SerialPort.devices.includesEqual(serport))
				// and if device is actually connected
				{
					"Device connected! Opening port!".postln;
					trackPort = SerialPort(serport, 115200, crtscts: true);
					this.trackerRoutine; // start tracker routine again
				}
			};
			1.wait;
		};
	})}

	/*
	offsetHeading { // give offset to reset North
		| angle |
		headingOffset = angle;
	}
	*/
}