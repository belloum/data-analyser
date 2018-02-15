package logtool.ui.resultpanels.settings;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;

import logtool.beans.Routine;
import logtool.beans.activityresults.ActivityResult;
import logtool.beans.activityresults.MealResult;
import logtool.beans.devices.Device;
import logtool.beans.devices.Device.DeviceType;
import logtool.operators.settings.MealSettings;
import logtool.operators.settings.RoutineSettings;
import logtool.ui.components.ComponentUtils;
import logtool.ui.components.MyButton;
import logtool.ui.dialogs.MarkerDialog;
import logtool.ui.dialogs.MarkerDialog.MarkerSelectionListener;

public class MealParametersPanel extends RoutineParameterPanel implements MarkerSelectionListener {

	private static final long serialVersionUID = 1L;

	private JLabel mMarkersI, mMarkersII;

	public MealParametersPanel(List<Device> pDevices, Routine pRoutine) {
		super(pDevices, pRoutine);
	}

	@Override
	public ProcessBuilder script(Routine pRoutine, File pFileToUse) throws Exception {
		validParameters();
		MealSettings ms = getRoutineSettings();
		return new ProcessBuilder(getScriptName(), String.format("-b%s", ms.getStartHour()),
				String.format("-e%s", ms.getEndHour()), String.format("-m%s", pRoutine.name()),
				String.format("-1%s",
						StringUtils.join(
								ms.getPrimaryMarkers().stream().map(d -> d.getId()).collect(Collectors.toList()), "|")),
				String.format("-2%s", StringUtils
						.join(ms.getSecondaryMarkers().stream().map(d -> d.getId()).collect(Collectors.toList()), "|")),
				pFileToUse.getAbsolutePath());
	}

	@Override
	public String getScriptName() {
		// TODO Make scripts configurable
		return "./meal.pl";
	}

	@SuppressWarnings("unchecked")
	@Override
	protected JPanel customParameters() {

		JPanel customSettings = new JPanel(new BorderLayout());

		mMarkersI = new JLabel();
		mMarkersII = new JLabel();

		JPanel labels = new JPanel(new GridLayout(2, 1));
		labels.add(ComponentUtils.boldLabel("Markers I\t"));
		labels.add(ComponentUtils.boldLabel("Markers II\t"));

		JPanel markers = new JPanel(new GridLayout(2, 1));
		markers.add(mMarkersI);
		markers.add(mMarkersII);

		customSettings.add(labels, BorderLayout.WEST);
		customSettings.add(markers, BorderLayout.CENTER);

		customSettings.add(
				new MyButton("Select markers",
						event -> new MarkerDialog(getRoutineDevices(),
								new List[] { getRoutineSettings().getPrimaryMarkers(),
										getRoutineSettings().getSecondaryMarkers() },
								this).build()),
				BorderLayout.EAST);

		return customSettings;
	}

	@Override
	protected RoutineSettings instantiateSettings() {
		return new MealSettings();
	}

	@Override
	public void setMarkers(List<Device> pPrimaryMarkers, List<Device> pSecondaryMarkers) {
		mMarkersI.setText(
				StringUtils.join(pPrimaryMarkers.stream().map(d -> d.getId()).collect(Collectors.toList()), ", "));
		mMarkersII.setText(
				StringUtils.join(pSecondaryMarkers.stream().map(d -> d.getId()).collect(Collectors.toList()), ", "));
		getRoutineSettings().setPrimaryMarkers(pPrimaryMarkers);
		getRoutineSettings().setSecondaryMarkers(pSecondaryMarkers);
	}

	@Override
	protected MealSettings getRoutineSettings() {
		return (MealSettings) super.mSettings;
	}

	@Override
	public List<ActivityResult> getResults(File pFileToUse) throws Exception {
		return Arrays.asList(executeScript(script(getRoutine(), pFileToUse))).stream().map(line -> new MealResult(line))
				.collect(Collectors.toList());
	}

	@Override
	protected List<Device> getRoutineDevices() {
		return super.getRoutineDevices().stream().filter(device -> device.getType().equals(DeviceType.ContactSensor)
				|| device.getType().equals(DeviceType.ElectricMeter)).collect(Collectors.toList());
	}

}
