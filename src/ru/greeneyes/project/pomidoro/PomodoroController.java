package ru.greeneyes.project.pomidoro;

import java.awt.Color;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

import javax.swing.ImageIcon;
import javax.swing.JLayeredPane;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

/**
 * @author ivanalx
 * @date 29.04.2010 13:32:06
 */
public class PomodoroController {
	private PomodoroForm form;

	private volatile PomodoroState state = PomodoroState.STOP;

	private volatile long lastTimeStart;

	private final int runTime;
	private final int breakTime;

	private int donePomodoroAmount = 0;
	private Project project;

	public PomodoroController(Project project, PomodoroForm form, int runTime, int breakTime) {
		this.form = form;
		this.runTime = runTime;
		this.breakTime = breakTime;
		this.project = project;
	}

	public void buttonPressed() {
		switch (state) {
		case RUN: {
			moveToStop();
			return;
		}
		case STOP: {
			moveToRun();
			return;
		}
		case BREAK: {
			moveToStop();
			return;
		}
		}
	}

	public void update() {
		long time = System.currentTimeMillis();
		switch (state) {
		case RUN: {
			if (time >= (lastTimeStart + runTime)) {
				updateTimer(lastTimeStart + runTime);
				moveToBreak();
				makeOneMorePomodoroDone();
			} else {
				updateTimer(time);
				updateProgressBarText("Working");
			}

			return;
		}
		case BREAK: {
			if (time >= (lastTimeStart + breakTime)) {
				updateTimer(lastTimeStart + breakTime);
				updateProgressBarText("Break is done");
				moveToStop();
			} else {
				updateTimer(time);
				updateProgressBarText("Break");
			}
			return;
		}
		case STOP: {
			return;
		}
		}
	}

	private void makeOneMorePomodoroDone() {
		donePomodoroAmount += 1;
		invokeAndWait(new Runnable() {
			public void run() {
				form.setPomodoroAmount(donePomodoroAmount);
			}
		});
	}

	private enum PomodoroState {
		STOP,
		RUN,
		BREAK
	}


	private void moveToBreak() {
		lastTimeStart = System.currentTimeMillis();
		invokeAndWait(new Runnable() {
			public void run() {
				makeButtonStop();
				form.getProgressBar1().setMaximum(breakTime / 1000);
				form.getProgressBar1().setValue(0);
				updateProgressBarText("Break");
				int s = getDonePomodoroAmount() + 1;
				createBaloon("You have done " + s + " Pomodoro" + ((s > 1)? "s":"") + ". Take break.");
			}
		});
		state = PomodoroState.BREAK;
	}

	private void makeButtonStop() {
		form.getControllButton().setText("Stop");
		form.getControllButton().setIcon(new ImageIcon(getClass().getResource("/ru/greeneyes/project/pomidoro/stop-icon.png")));
	}

	private void makeButtonStart() {
		form.getControllButton().setText("Play");
		form.getControllButton().setIcon(new ImageIcon(getClass().getResource("/ru/greeneyes/project/pomidoro/play-icon.png")));
	}


	private void moveToStop() {
		lastTimeStart = 0;
		invokeAndWait(new Runnable() {
			public void run() {
				makeButtonStart();
			}
		});
		state = PomodoroState.STOP;
	}


	private void moveToRun() {
		lastTimeStart = System.currentTimeMillis();
		invokeAndWait(new Runnable() {
			public void run() {
				makeButtonStop();
				form.getProgressBar1().setMaximum(runTime / 1000);
				form.getProgressBar1().setValue(0);
				updateProgressBarText("Working");
			}
		});
		state = PomodoroState.RUN;
	}

	private void createBaloon(String text) {
		/*
		JRootPane rp = SwingUtilities.getRootPane(form.getRootPanel());
		IdeFrame ideFrame = WindowManager.getInstance().getAllFrames()[0];

		JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text, MessageType.INFO, null)
					.setFadeoutTime(20000)
					.setBorderColor(Color.WHITE)
					.setCloseButtonEnabled(true)
					.createBalloon().show(null, Balloon.Position.atRight);
					*/
	}

	private void updateProgressBarText(final String prefix) {
		final JProgressBar pb = form.getProgressBar1();
		int value = pb.getValue();
		final int min = value / 60;
		final int sec = value % 60;

		invokeAndWait(new Runnable() {
			public void run() {
				pb.setString(prefix + ": " + min + ":" + ((sec < 10)? ("0"+ sec): (sec)));
			}
		});
	}


	private void updateTimer(final long time) {
		invokeAndWait(new Runnable() {
			public void run() {
				JProgressBar pb = form.getProgressBar1();
				pb.setValue(pb.getMaximum() - (int) (time - lastTimeStart) / 1000);
			}
		});
	}

	private static void invokeAndWait(Runnable r) {
		if (!SwingUtilities.isEventDispatchThread()) {
			try {
				SwingUtilities.invokeAndWait(r);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		} else {
			r.run();
		}
	}

	public int getDonePomodoroAmount() {
		return donePomodoroAmount;
	}
}
