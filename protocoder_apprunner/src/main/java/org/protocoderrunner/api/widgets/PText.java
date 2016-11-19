/*
* Part of Protocoder http://www.protocoder.org
* A prototyping platform for Android devices 
*
* Copyright (C) 2013 Victor Diaz Barrales victormdb@gmail.com
* 
* Protocoder is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Protocoder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU Lesser General Public License
* along with Protocoder. If not, see <http://www.gnu.org/licenses/>.
*/

package org.protocoderrunner.api.widgets;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import org.protocoderrunner.apidoc.annotation.ProtoMethod;
import org.protocoderrunner.apidoc.annotation.ProtoMethodParam;
import org.protocoderrunner.apprunner.AppRunner;
import org.protocoderrunner.apprunner.StyleProperties;

import java.util.Map;

public class PText extends TextView implements PViewMethodsInterface, PTextInterface {

    public StyleProperties props = new StyleProperties();
    public Styler styler;

    public PText(AppRunner appRunner) {
        super(appRunner.getAppContext());
        styler = new Styler(appRunner, this, props);
        styler.apply();
    }

    @ProtoMethod(description = "Sets the text color", example = "")
    @ProtoMethodParam(params = {"colorHex"})
    public PText color(String c) {
        this.setTextColor(Color.parseColor(c));

        return this;
    }

    @ProtoMethod(description = "Sets the background color", example = "")
    @ProtoMethodParam(params = {"colorHex"})
    public PText background(String c) {
        this.setBackgroundColor(Color.parseColor(c));
        return this;
    }

    @ProtoMethod(description = "Enables/disables the scroll in the text view", example = "")
    @ProtoMethodParam(params = {"size"})
    public PText scrollable(boolean b) {
        if (b) {
            this.setMovementMethod(new ScrollingMovementMethod());
            this.setVerticalScrollBarEnabled(true);
            // this.setGravity(Gravity.BOTTOM);
        } else {
            this.setMovementMethod(null);
        }
        return this;
    }

    @ProtoMethod(description = "Changes the text to the given text", example = "")
    @ProtoMethodParam(params = {"text"})
    public PText text(String text) {
        this.setText(text);
        return this;
    }

    @ProtoMethod(description = "Changes the text to the given text", example = "")
    @ProtoMethodParam(params = {"text, text, ..., text"})
    public PText text(String... txt) {
        String joinedText = "";
        for (int i = 0; i < txt.length; i++) {
            joinedText += " " + txt[i];
        }
        this.setText(joinedText);

        return this;
    }

    @ProtoMethod(description = "Changes the text to the given html text", example = "")
    @ProtoMethodParam(params = {"htmlText"})
    public PText html(String text) {
        this.setText(Html.fromHtml(text));
        return this;
    }

    @ProtoMethod(description = "Appends text to the text view", example = "")
    @ProtoMethodParam(params = {"text"})
    public PText append(String text) {
        this.setText(getText() + text);

        return this;
    }

    @ProtoMethod(description = "Clears the text", example = "")
    public PText clear() {
        this.setText("");
        return this;
    }

    @ProtoMethod(description = "Changes the box size of the text", example = "")
    @ProtoMethodParam(params = {"w", "h"})
    public PText boxsize(int w, int h) {
        this.setWidth(w);
        this.setHeight(h);
        return this;
    }

    @ProtoMethod(description = "Sets a new position for the text", example = "")
    @ProtoMethodParam(params = {"x", "y"})
    public PText pos(int x, int y) {
        this.setX(x);
        this.setY(y);
        return this;
    }

    @ProtoMethod(description = "Specifies a shadow for the text", example = "")
    @ProtoMethodParam(params = {"x", "y", "radius", "colorHex"})
    public PText shadow(int x, int y, int r, String c) {
        this.setShadowLayer(r, x, y, Color.parseColor(c));
        return this;
    }

    @ProtoMethod(description = "Centers the text inside the textview", example = "")
    @ProtoMethodParam(params = {"Typeface"})
    public PText center(String centering) {
        this.setGravity(Gravity.CENTER_VERTICAL);
        return this;
    }

    @ProtoMethod(description = "Changes the font", example = "")
    @ProtoMethodParam(params = {"Typeface"})
    public PText font(Typeface f) {
        this.setTypeface(f);
        return this;
    }

    @ProtoMethod(description = "Sets the text size", example = "")
    @ProtoMethodParam(params = {"size"})
    public PText textSize(int size) {
        this.setTextSize(size);
        return this;
    }

    @Override
    public View textColor(String textColor) {
        this.setTextColor(Color.parseColor(textColor));
        return this;
    }

    @Override
    @ProtoMethod(description = "Changes the font text color", example = "")
    @ProtoMethodParam(params = {"colorHex"})
    public View textColor(int c) {
        this.setTextColor(c);
        return this;
    }

    @Override
    public View textSize(float textSize) {
        this.setTextSize(textSize);
        return this;
    }

    @Override
    public View textStyle(int style) {
        this.setTypeface(null, style);
        return this;
    }

    @Override
    public void set(float x, float y, float w, float h) {
        styler.setLayoutProps(x, y, w, h);
    }

    @Override
    public void setStyle(Map style) {
        styler.setStyle(style);
    }

    @Override
    public Map getStyle() {
        return props;
    }

    /*
    public PTextView center(String how) {
        this.setTextAlignment(TEXT_ALIGNMENT_TEXT_START);
        this.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        this.setTextAlignment(TEXT_ALIGNMENT_TEXT_END);
        this.setTextAlignment(TEXT_ALIGNMENT);
    }
    */
}
