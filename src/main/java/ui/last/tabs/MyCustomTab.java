package ui.last.tabs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;

import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import operators.extractors.FileExtractor;
import ui.last.components.ErrorLabel;
import ui.last.components.TabHeader;
import utils.Configuration;
import utils.Utils;

/**
 * Force tabs to have a header
 */
public abstract class MyCustomTab extends JPanel implements ConfigurableTab {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private TabHeader mHeader;
	private final ErrorLabel mError;
	private JSONObject mSettings;

	public MyCustomTab() {
		setLayout(new BorderLayout());

		try {
			mSettings = configuration();
		} catch (final Exception e) {
			final String error = String.format("Unable to find configuration file (%s): %s", this.getClass().getName(),
					e.getMessage());
			Utils.errorLog(error);
			System.err.println(error);
		}

		mHeader = header();

		add(mHeader, BorderLayout.PAGE_START);
		mError = new ErrorLabel();
		add(mError, BorderLayout.PAGE_END);
	}

	protected abstract Component content();

	protected TabHeader header() {
		return new TabHeader(settings().getString("title"), settings().getString("description"),
				new File(Configuration.IMAGES_FOLDER, settings().getString("img")));
	}

	protected void error(final String pErrorMsg) {
		if (!StringUtils.isEmpty(pErrorMsg)) {
			Utils.errorLog(pErrorMsg);
			System.err.println("Error: " + pErrorMsg);
			mError.setVisible(true);
			mError.setText(pErrorMsg);
			validate();
		}
	}

	protected void hideError() {
		mError.setVisible(false);
		validate();
	}

	protected TabHeader getHeader() {
		return this.mHeader;
	}

	protected JSONObject settings() {
		return this.mSettings;
	}

	@Override
	public JSONObject configuration() throws Exception {
		return new JSONObject(FileExtractor.readFile(ConfigurableTab.configurationFile())).getJSONObject("tabs")
				.getJSONObject(configurationSection());
	}

}
