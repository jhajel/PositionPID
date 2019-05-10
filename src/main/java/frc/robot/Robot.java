/**
 * Phoenix Software License Agreement
 *
 * Copyright (C) Cross The Road Electronics.  All rights
 * reserved.
 * 
 * Cross The Road Electronics (CTRE) licenses to you the right to 
 * use, publish, and distribute copies of CRF (Cross The Road) firmware files (*.crf) and 
 * Phoenix Software API Libraries ONLY when in use with CTR Electronics hardware products
 * as well as the FRC roboRIO when in use in FRC Competition.
 * 
 * THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT
 * WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT
 * LIMITATION, ANY WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT SHALL
 * CROSS THE ROAD ELECTRONICS BE LIABLE FOR ANY INCIDENTAL, SPECIAL, 
 * INDIRECT OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF
 * PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY OR SERVICES, ANY CLAIMS
 * BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE
 * THEREOF), ANY CLAIMS FOR INDEMNITY OR CONTRIBUTION, OR OTHER
 * SIMILAR COSTS, WHETHER ASSERTED ON THE BASIS OF CONTRACT, TORT
 * (INCLUDING NEGLIGENCE), BREACH OF WARRANTY, OR OTHERWISE
 */

/**
 * Description:
 * The PositiionClosedLoop_AuxStraightQuadrature example demonstrates the new Talon/Victor Auxiliary
 * and Remote Features to peform more complex. This example has the robot performing Position 
 * Closed Loop with an auxiliary closed loop on Quadrature Encoders difference (Heading)
 * to keep the robot straight.
 * 
 * This example uses:
 * - 2x Quad Encoders, one on both sides of robot for Primary Closed Loop on Position
 * A Talon/Victor calculates the distance by taking the sum of both sensors and dividing it by 2.
 * - 2x Quad Encoders, one of both side of robot for Auxiliary Closed Loop on Heading
 * A Talon/Victor calculates the heading by taking the difference between both sensors
 * 
 * This example has two modes of operation, which can be switched between with Button 2.
 * 1.) Arcade Drive
 * 2.) Position Closed Loop wtih Quadrature Encoders and Drive Straight with Quadrature
 * Encoders difference
 * 
 * Controls:
 * Button 1: When pressed, zero all sensors. Set Quadrature Encoders positions to 0
 * Button 2: When pressed, Toggle between Arcade Drive and Position Closed Loop.
 * 	When toggling into Position Closed Loop, the current heading is saved and used as the 
 * auxiliary closed loop target. Can be changed by toggling out and in again.
 * Left Joystick Y-Axis: 
 * 	+ Arcade Drive: Drive robot forward and reverse
 * 	+ Position Closed Loop: Servo robot forward and reverse [-6, 6] rotations
 * Right Joystick X-Axis:
 * 	 + Arcade Drive: Turn robot left and right
 * 	+ Position Closed Loop: Not used
 * 
 * Gains for Position Closed Loop and Auxiliary may need to be adjusted in Constants.java
 * 
 * Supported Version:
 * - Talon SRX: 4.00
 * - Victor SPX: 4.00
 * - Pigeon IMU: 4.00
 * - CANifier: 4.00
 */
package frc.robot;

import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.SPI;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.*;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.shuffleboard.*;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.RemoteSensorSource;
import com.ctre.phoenix.motorcontrol.SensorTerm;
import com.ctre.phoenix.motorcontrol.StatusFrame;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FollowerType;
import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;

public class Robot extends TimedRobot {

	/** NavX */

	AHRS navX = new AHRS(SPI.Port.kMXP);

	/** Hardware */
	WPI_TalonSRX _leftMaster = new WPI_TalonSRX(6);
	WPI_TalonSRX _rightMaster = new WPI_TalonSRX(1);
  

	WPI_TalonSRX _rightFollower = new WPI_TalonSRX(2);
	WPI_TalonSRX _rightFollower2 = new WPI_TalonSRX(3);

	WPI_TalonSRX _leftFollower = new WPI_TalonSRX(5);
	WPI_TalonSRX _leftFollower2 = new WPI_TalonSRX(4);
	XboxController _gamepad = new XboxController(0);
	
	/** Latched values to detect on-press events for buttons */
	boolean[] _btns = new boolean[Constants.kNumButtonsPlusOne];
	boolean[] btns = new boolean[Constants.kNumButtonsPlusOne];
	
	/** Tracking variables */
	boolean _firstCall = false;
	boolean _state = false;
	double _lockedDistance = 0;
	double _targetAngle = 0;

	/** to retrieve data from the Shuffleboad */
	private ShuffleboardTab tab = Shuffleboard.getTab("Autonomous Testing");

	private NetworkTableEntry angleEntry = tab.add("Request angle", 10000).getEntry();
	private NetworkTableEntry positionEntry = tab.add("Requested Position", 10000).getEntry();

	@Override
	public void robotInit() {
		/* Not used in this project */
	}
	
	@Override
	public void teleopInit(){

    _rightFollower.follow(_rightMaster);
    _leftFollower.follow(_leftMaster);
    _rightFollower2.follow(_rightMaster);
    _leftFollower2.follow(_leftMaster);
		/* Disable all motor controllers */
		_rightMaster.set(ControlMode.Position, 0);
		_leftMaster.set(ControlMode.Position, 0);
		

		/* Disable all motor controllers */
		_rightMaster.set(ControlMode.PercentOutput, 0);
		_leftMaster.set(ControlMode.PercentOutput, 0);

		/* Factory Default all hardware to prevent unexpected behaviour */
		_rightMaster.configFactoryDefault();
		_leftMaster.configFactoryDefault();
		
		/* Set Neutral Mode */
		/* Set Neutral Mode */
		_leftMaster.setNeutralMode(NeutralMode.Brake);
		_rightMaster.setNeutralMode(NeutralMode.Brake);
		_leftFollower.setNeutralMode(NeutralMode.Brake);
		_rightFollower.setNeutralMode(NeutralMode.Brake);
		_leftFollower2.setNeutralMode(NeutralMode.Brake);
		_rightFollower2.setNeutralMode(NeutralMode.Brake);
		
		/** Feedback Sensor Configuration */
		
		/* Configure the left Talon's selected sensor as local QuadEncoder */
		
		_leftMaster.configSelectedFeedbackSensor(	FeedbackDevice.QuadEncoder,				// Local Feedback Source
													Constants.PID_PRIMARY,					// PID Slot for Source [0, 1]
													Constants.kTimeoutMs);					// Configuration Timeout

		/* Configure the Remote Talon's selected sensor as a remote sensor for the right Talon */
		_rightMaster.configRemoteFeedbackFilter(_leftMaster.getDeviceID(),					// Device ID of Source
												RemoteSensorSource.TalonSRX_SelectedSensor,	// Remote Feedback Source
												Constants.REMOTE_0,							// Source number [0, 1]
												Constants.kTimeoutMs);						// Configuration Timeout




		/* Setup Sum signal to be used for Distance */
		_rightMaster.configSensorTerm(SensorTerm.Sum0, FeedbackDevice.RemoteSensor0, Constants.kTimeoutMs);	// Feedback Device of Remote Talon
		_rightMaster.configSensorTerm(SensorTerm.Sum1, FeedbackDevice.QuadEncoder, Constants.kTimeoutMs);	// Quadrature Encoder of current Talon
		
		/* Setup Difference signal to be used for Turn */
		_rightMaster.configSensorTerm(SensorTerm.Diff1, FeedbackDevice.RemoteSensor0, Constants.kTimeoutMs);
		_rightMaster.configSensorTerm(SensorTerm.Diff0, FeedbackDevice.QuadEncoder, Constants.kTimeoutMs);
		
		/* Configure Sum [Sum of both QuadEncoders] to be used for Primary PID Index */
		_rightMaster.configSelectedFeedbackSensor(	FeedbackDevice.SensorSum, 
													Constants.PID_PRIMARY,
													Constants.kTimeoutMs);
		
		/* Scale Feedback by 0.5 to half the sum of Distance */
		_rightMaster.configSelectedFeedbackCoefficient(	0.5, 						// Coefficient
														Constants.PID_PRIMARY,		// PID Slot of Source 
														Constants.kTimeoutMs);		// Configuration Timeout
		
		/* Configure Difference [Difference between both QuadEncoders] to be used for Auxiliary PID Index */
		_rightMaster.configSelectedFeedbackSensor(	FeedbackDevice.SensorDifference, 
													Constants.PID_TURN, 
													Constants.kTimeoutMs);
		
		/* Scale the Feedback Sensor using a coefficient */
		_rightMaster.configSelectedFeedbackCoefficient(	1,
														Constants.PID_TURN, 
														Constants.kTimeoutMs);


		
		/* Configure output and sensor direction */
		_leftMaster.setInverted(false);
		_leftMaster.setSensorPhase(true);
		_leftFollower.setInverted(false);
		_leftFollower.setSensorPhase(true);
		_leftFollower2.setInverted(false);
		_leftFollower2.setSensorPhase(true);
		

		_rightMaster.setInverted(true);
		_rightMaster.setSensorPhase(true);
		_rightFollower.setInverted(true);
		_rightFollower.setSensorPhase(true);
		_rightFollower2.setInverted(true);
		_rightFollower2.setSensorPhase(true);
		
		/* Set status frame periods to ensure we don't have stale data */
		_rightMaster.setStatusFramePeriod(StatusFrame.Status_12_Feedback1, 20, Constants.kTimeoutMs);
		_rightMaster.setStatusFramePeriod(StatusFrame.Status_13_Base_PIDF0, 20, Constants.kTimeoutMs);
		_rightMaster.setStatusFramePeriod(StatusFrame.Status_14_Turn_PIDF1, 20, Constants.kTimeoutMs);
		_rightMaster.setStatusFramePeriod(StatusFrame.Status_10_Targets, 20, Constants.kTimeoutMs);
		_leftMaster.setStatusFramePeriod(StatusFrame.Status_2_Feedback0, 5, Constants.kTimeoutMs);

		/* Configure neutral deadband */
		_rightMaster.configNeutralDeadband(Constants.kNeutralDeadband, Constants.kTimeoutMs);
		_leftMaster.configNeutralDeadband(Constants.kNeutralDeadband, Constants.kTimeoutMs);

		/* Max out the peak output (for all modes).  
		 * However you can limit the output of a given PID object with configClosedLoopPeakOutput().
		 */
		_leftMaster.configPeakOutputForward(+1.0, Constants.kTimeoutMs);
		_leftMaster.configPeakOutputReverse(-1.0, Constants.kTimeoutMs);
		_rightMaster.configPeakOutputForward(+1.0, Constants.kTimeoutMs);
		_rightMaster.configPeakOutputReverse(-1.0, Constants.kTimeoutMs);

		/* FPID Gains for distance servo */
		_rightMaster.config_kP(Constants.kSlot_Distanc, Constants.kGains_Distanc.kP, Constants.kTimeoutMs);
		_rightMaster.config_kI(Constants.kSlot_Distanc, Constants.kGains_Distanc.kI, Constants.kTimeoutMs);
		_rightMaster.config_kD(Constants.kSlot_Distanc, Constants.kGains_Distanc.kD, Constants.kTimeoutMs);
		_rightMaster.config_kF(Constants.kSlot_Distanc, Constants.kGains_Distanc.kF, Constants.kTimeoutMs);
		_rightMaster.config_IntegralZone(Constants.kSlot_Distanc, Constants.kGains_Distanc.kIzone, Constants.kTimeoutMs);
		_rightMaster.configClosedLoopPeakOutput(Constants.kSlot_Distanc, Constants.kGains_Distanc.kPeakOutput, Constants.kTimeoutMs);
		_rightMaster.configAllowableClosedloopError(Constants.kSlot_Distanc, 0, Constants.kTimeoutMs);

		/* FPID Gains for turn servo */
		_rightMaster.config_kP(Constants.kSlot_Turning, Constants.kGains_Turning.kP, Constants.kTimeoutMs);
		_rightMaster.config_kI(Constants.kSlot_Turning, Constants.kGains_Turning.kI, Constants.kTimeoutMs);
		_rightMaster.config_kD(Constants.kSlot_Turning, Constants.kGains_Turning.kD, Constants.kTimeoutMs);
		_rightMaster.config_kF(Constants.kSlot_Turning, Constants.kGains_Turning.kF, Constants.kTimeoutMs);
		_rightMaster.config_IntegralZone(Constants.kSlot_Turning, Constants.kGains_Turning.kIzone, Constants.kTimeoutMs);
		_rightMaster.configClosedLoopPeakOutput(Constants.kSlot_Turning, Constants.kGains_Turning.kPeakOutput, Constants.kTimeoutMs);
			
		_rightMaster.configAllowableClosedloopError(Constants.kSlot_Turning, 0, Constants.kTimeoutMs);


		/* 1ms per loop.  PID loop can be slowed down if need be.
		 * For example,
		 * - if sensor updates are too slow
		 * - sensor deltas are very small per update, so derivative error never gets large enough to be useful.
		 * - sensor movement is very slow causing the derivative error to be near zero.
		 */
        int closedLoopTimeMs = 1;
        _rightMaster.configClosedLoopPeriod(0, closedLoopTimeMs, Constants.kTimeoutMs);
        _rightMaster.configClosedLoopPeriod(1, closedLoopTimeMs, Constants.kTimeoutMs);

		/* configAuxPIDPolarity(boolean invert, int timeoutMs)
		 * false means talon's local output is PID0 + PID1, and other side Talon is PID0 - PID1
		 * true means talon's local output is PID0 - PID1, and other side Talon is PID0 + PID1
		 */
		_rightMaster.configAuxPIDPolarity(false, Constants.kTimeoutMs);

		/* Initialize */
		_firstCall = true;
		_state = false;
		_rightMaster.setStatusFramePeriod(StatusFrameEnhanced.Status_10_Targets, 10);

		zeroSensors();
		_rightMaster.configMotionSCurveStrength(8);
		_rightMaster.configMotionCruiseVelocity(400);
		_rightMaster.configMotionAcceleration(100);

	}
	
	@Override
	public void teleopPeriodic() {
		/* Gamepad processing */
		double forward = -1 * _gamepad.getRawAxis(1);
		double turn = _gamepad.getRawAxis(4);
		forward = Deadband(forward);
		turn = Deadband(turn);
	
		
		//SmartDashboard.putNumber("Forward Value", forward);
		//SmartDashboard.putNumber("Yaw", navX.getYaw());
		


		/* Button processing for state toggle and sensor zeroing */
		getButtons(btns, _gamepad);
		if(btns[2] && !_btns[2]){
			_state = !_state; 		// Toggle state
			_firstCall = true;		// State change, do first call operation
			_targetAngle = _rightMaster.getSelectedSensorPosition(1);
			_lockedDistance = _rightMaster.getSelectedSensorPosition(0);
		}else if (btns[1] && !_btns[1]) {
			zeroSensors();			// Zero Sensors
		}
		System.arraycopy(btns, 0, _btns, 0, Constants.kNumButtonsPlusOne);
		
		double encoderPosition = _rightMaster.getSensorCollection().getQuadraturePosition();
		double encoderPositionLeft = _leftMaster.getSensorCollection().getQuadraturePosition();

		SmartDashboard.putNumber("Right raw Position", encoderPosition);
		SmartDashboard.putNumber("Left raw Position", encoderPositionLeft);
		SmartDashboard.putNumber("Encoder angle", _rightMaster.getSelectedSensorPosition(1));
		SmartDashboard.putNumber("Encoder Pos sum", _rightMaster.getSelectedSensorPosition(0));
		SmartDashboard.putNumber("Yaw", navX.getYaw());
		SmartDashboard.putNumber("Speed", _rightMaster.getSelectedSensorVelocity(0));

		if(!_state){
			if (_firstCall)
				System.out.println("This is a Arcade Drive.\n");
			
			_leftMaster.set(ControlMode.PercentOutput, forward, DemandType.ArbitraryFeedForward, +turn);
			_rightMaster.set(ControlMode.PercentOutput, forward, DemandType.ArbitraryFeedForward, -turn);
		}else{
			if (_firstCall) {
				System.out.println("This is Drive Straight Distance with the Auxiliary PID using the difference between two encoders.");
				System.out.println("Servo [-6, 6] rotations while also maintaining a straight heading.\n");
				
				/* Determine which slot affects which PID */
				_rightMaster.selectProfileSlot(Constants.kSlot_Distanc, Constants.PID_PRIMARY);
				_rightMaster.selectProfileSlot(Constants.kSlot_Turning, Constants.PID_TURN);
			}
			
			/* Calculate targets from gamepad inputs */
			//double target_sensorUnits = forward * Constants.kSensorUnitsPerRotation * Constants.kRotationsToTravel  + _lockedDistance;
			//double target_turn = _targetAngle;
			
			double position = positionEntry.getDouble(1000.0);  //get desired position from Shuffleboard
			double angle = angleEntry.getDouble(0.0);  //get desired angle from Shuffleboard
			/* Configured for Position Closed loop on Quad Encoders' Sum and Auxiliary PID on Quad Encoders' Difference */
			_rightMaster.configMotionSCurveStrength(4);
			//System.out.println("pos:"+ position);
			_rightMaster.set(ControlMode.MotionMagic, position, DemandType.AuxPID, angle);
			_leftMaster.follow(_rightMaster, FollowerType.AuxOutput1);
		}
		_firstCall = false;
	}
	
	/* Zero quadrature encoders on Talons */
	void zeroSensors() {
		_leftMaster.getSensorCollection().setQuadraturePosition(0, Constants.kTimeoutMs);
		_rightMaster.getSensorCollection().setQuadraturePosition(0, Constants.kTimeoutMs);
		System.out.println("[Quadrature Encoders] All sensors are zeroed.\n");
	}
	
	/** Deadband 5 percent, used on the gamepad */
	double Deadband(double value) {
		/* Upper deadband */
		if (value >= +0.1) 
			return value;
		
		/* Lower deadband */
		if (value <= -0.1)
			return value;
		
		/* Outside deadband */
		return 0;
	}
	
	/** Gets all buttons from gamepad */
	void getButtons(boolean[] btns, XboxController gamepad) {
		for (int i = 1; i < Constants.kNumButtonsPlusOne; ++i) {
			btns[i] = gamepad.getRawButton(i);
		}
	}
}