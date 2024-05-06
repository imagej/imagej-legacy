/*
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2024 ImageJ2 developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.legacy.ui;

import net.imagej.legacy.LegacyService;

import org.scijava.ui.DialogPrompt;

/**
 * {@link LegacyAdapter} implementation for adapting between
 * {@link DialogPrompt} and the ImageJ1 dialog prompts.
 * 
 * @author Mark Hiner
 */
public class LegacyDialogPrompt extends AbstractLegacyAdapter implements
	DialogPrompt
{

	private final String message;
	private final String title;
	private final OptionType optionType;

	public LegacyDialogPrompt(final LegacyService service, final String message,
		final String title, final OptionType optionType)
	{
		super(service);
		this.message = message;
		this.title = title;
		this.optionType = optionType;
	}

	@Override
	public Result prompt() {
		switch (optionType) {
			case DEFAULT_OPTION:
				helper().showMessage(title, message);
				return Result.OK_OPTION;
			case OK_CANCEL_OPTION:
				return helper().showMessageWithCancel(title, message)
					? Result.OK_OPTION : Result.CANCEL_OPTION;
			case YES_NO_OPTION:
				return helper().showMessageWithCancel(title, message)
					? Result.YES_OPTION : Result.CANCEL_OPTION;
			case YES_NO_CANCEL_OPTION:
				return helper().showMessageWithCancel(title, message)
					? Result.YES_OPTION : Result.CANCEL_OPTION;
				// FIXME: scijava-ui-awt should extend a Swing or AWT dialog prompt.
				// Then return super.prompt(). Pass fields to super constructor.
			default:
				throw new UnsupportedOperationException();
		}
	}

}
