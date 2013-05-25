package org.alliance.ui.themes;

import org.pushingpixels.substance.api.ComponentState;
import org.pushingpixels.substance.api.DecorationAreaType;
import org.pushingpixels.substance.api.SubstanceColorScheme;
import org.pushingpixels.substance.api.SubstanceColorSchemeBundle;
import org.pushingpixels.substance.api.SubstanceSkin;
import org.pushingpixels.substance.api.painter.border.ClassicBorderPainter;
import org.pushingpixels.substance.api.painter.decoration.ArcDecorationPainter;
import org.pushingpixels.substance.api.painter.decoration.BrushedMetalDecorationPainter;
import org.pushingpixels.substance.api.painter.highlight.ClassicHighlightPainter;
import org.pushingpixels.substance.api.painter.overlay.BottomShadowOverlayPainter;
import org.pushingpixels.substance.api.painter.overlay.TopShadowOverlayPainter;
import org.pushingpixels.substance.api.shaper.ClassicButtonShaper;

import java.io.File;
import org.pushingpixels.substance.api.painter.fill.GlassFillPainter;

/**
 *
 * @author Bastvera
 */
public class AllianceTheme extends SubstanceSkin {

    public static final String NAME = "Alliance";

    public AllianceTheme() {
        SubstanceSkin.ColorSchemes schemes;
        try {
            File schemeFile = new File("skin/alliance/alliance.colorschemes");
            schemes = SubstanceSkin.getColorSchemes(schemeFile.toURI().toURL());
        } catch (Exception ex) {
            return;
        }

        SubstanceColorScheme activeScheme = schemes.get("Alliance Active");
        SubstanceColorScheme defaultScheme = schemes.get("Alliance Default");
        SubstanceColorScheme disabledScheme = schemes.get("Alliance Disabled");

        // the default color scheme bundle
        SubstanceColorSchemeBundle defaultSchemeBundle = new SubstanceColorSchemeBundle(
                activeScheme, defaultScheme, disabledScheme);
        defaultSchemeBundle.registerHighlightColorScheme(activeScheme, 0.6f,
                ComponentState.ROLLOVER_UNSELECTED);
        defaultSchemeBundle.registerHighlightColorScheme(activeScheme, 0.8f,
                ComponentState.SELECTED);
        defaultSchemeBundle.registerHighlightColorScheme(activeScheme, 0.95f,
                ComponentState.ROLLOVER_SELECTED);
        defaultSchemeBundle.registerHighlightColorScheme(activeScheme, 0.8f,
                ComponentState.ARMED, ComponentState.ROLLOVER_ARMED);
        this.registerDecorationAreaSchemeBundle(defaultSchemeBundle,
                DecorationAreaType.NONE);

        // color scheme bundle for title panes
        SubstanceColorScheme activeHeaderScheme = schemes.get("Alliance Header and Buttons");
        SubstanceColorScheme defaultHeaderScheme = schemes.get("Alliance Header and Buttons");
        SubstanceColorScheme rolloverHeaderScheme = schemes.get("Alliance Header Rollover");
        SubstanceColorSchemeBundle headerSchemeBundle = new SubstanceColorSchemeBundle(
                activeHeaderScheme, defaultHeaderScheme, disabledScheme);
        headerSchemeBundle.registerHighlightColorScheme(rolloverHeaderScheme, 0.6f,
                ComponentState.ROLLOVER_UNSELECTED);
        headerSchemeBundle.registerHighlightColorScheme(rolloverHeaderScheme, 0.8f,
                ComponentState.SELECTED);
        headerSchemeBundle.registerHighlightColorScheme(rolloverHeaderScheme, 0.95f,
                ComponentState.ROLLOVER_SELECTED);
        headerSchemeBundle.registerHighlightColorScheme(rolloverHeaderScheme, 0.8f,
                ComponentState.ARMED, ComponentState.ROLLOVER_ARMED);

        this.registerDecorationAreaSchemeBundle(headerSchemeBundle,
                activeHeaderScheme, DecorationAreaType.PRIMARY_TITLE_PANE,
                DecorationAreaType.SECONDARY_TITLE_PANE,
                DecorationAreaType.HEADER);

        // color scheme bundle for general areas
        SubstanceColorScheme activeGeneralScheme = schemes.get("Alliance Header and Buttons");
        SubstanceColorScheme defaultGeneralScheme = schemes.get("Alliance Header and Buttons");
        SubstanceColorSchemeBundle generalSchemeBundle = new SubstanceColorSchemeBundle(
                activeGeneralScheme, defaultGeneralScheme, disabledScheme);
        generalSchemeBundle.registerColorScheme(schemes.get("Alliance Buttons Rollover"),
                ComponentState.ROLLOVER_SELECTED,
                ComponentState.ROLLOVER_UNSELECTED,
                ComponentState.ROLLOVER_ARMED);
        this.registerDecorationAreaSchemeBundle(generalSchemeBundle, DecorationAreaType.GENERAL);

        SubstanceColorScheme footerScheme = schemes.get("Alliance Infonode MDI");
        SubstanceColorSchemeBundle footerSchemeBundle = new SubstanceColorSchemeBundle(
                footerScheme, footerScheme, footerScheme);
        this.registerDecorationAreaSchemeBundle(footerSchemeBundle, DecorationAreaType.FOOTER);

        // add an overlay painter to paint a drop shadow
        this.addOverlayPainter(TopShadowOverlayPainter.getInstance(), DecorationAreaType.TOOLBAR);
        this.addOverlayPainter(BottomShadowOverlayPainter.getInstance(), DecorationAreaType.PRIMARY_TITLE_PANE);

        this.buttonShaper = new ClassicButtonShaper();
        this.fillPainter = new GlassFillPainter();
        this.borderPainter = new ClassicBorderPainter();

        BrushedMetalDecorationPainter decorationPainter = new BrushedMetalDecorationPainter();
        decorationPainter.setBaseDecorationPainter(new ArcDecorationPainter());
        this.decorationPainter = decorationPainter;

        this.highlightPainter = new ClassicHighlightPainter();
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }
}
