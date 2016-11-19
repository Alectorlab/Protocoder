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
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import org.protocoderrunner.api.common.ReturnInterface;
import org.protocoderrunner.api.common.ReturnObject;
import org.protocoderrunner.apidoc.annotation.ProtoMethod;
import org.protocoderrunner.apidoc.annotation.ProtoMethodParam;
import org.protocoderrunner.apprunner.AppRunner;
import org.protocoderrunner.apprunner.StyleProperties;

import java.util.Map;

public class PToggle extends ToggleButton implements PViewMethodsInterface, PTextInterface {

    public StyleProperties props = new StyleProperties();
    private Styler styler;

    public PToggle(AppRunner appRunner) {
        super(appRunner.getAppContext());

        styler = new Styler(appRunner, this, props);
        styler.apply();
    }

    public PToggle onChange(final ReturnInterface callbackfn) {
        // Add change listener
        this.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ReturnObject r = new ReturnObject(PToggle.this);
                r.put("checked", isChecked);
                callbackfn.event(r);
            }
        });

        return this;
    }

    public void text(String label) {
        this.props.put("text", this.props, label);
        setText(label);
        setTextOn(label);
        setTextOff(label);
    }

    public void checked(boolean b) {
        this.props.put("checked", this.props, b);
        setChecked(b);
    }

    @Override
    public View font(Typeface font) {
        this.setTypeface(font);
        return this;
    }

    @Override
    public View textSize(int size) {
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

}
