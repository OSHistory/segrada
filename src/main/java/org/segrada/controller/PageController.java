package org.segrada.controller;

import com.google.inject.Singleton;
import com.sun.jersey.api.view.Viewable;
import org.parboiled.common.StringUtils;
import org.pegdown.FastEncoder;
import org.pegdown.LinkRenderer;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.ExpLinkNode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Copyright 2015 Maximilian Kalus [segrada@auxnet.de]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Controller for nodes
 */
@Path("/page")
@Singleton
public class PageController {
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Viewable index(@Context HttpServletRequest request) {
		return getPage("index", request);
	}

	@GET
	@Path("/{page}")
	@Produces(MediaType.TEXT_HTML)
	public Viewable show(@PathParam("page") String page, @Context HttpServletRequest request) {
		return getPage(page, request);
	}

	/**
	 * get page from resource
	 * @param page id
	 * @return rendered view
	 */
	private Viewable getPage(String page, HttpServletRequest request) {
		HttpSession session = request.getSession();

		// get language from session
		Object lObject = session.getAttribute("language");
		String language = lObject==null?Locale.getDefault().getLanguage():(String) lObject;

		// create model map
		Map<String, Object> model = new HashMap<>();
		// add rendered page content
		model.put("page", renderPageContent(page, language, request.getContextPath()));
		model.put("pageId", page);
		model.put("language", language);

		return new Viewable("page", model);
	}

	/**
	 * render markdown content
	 * @param page id
	 * @param language language
	 * @param contextPath contextPath
	 * @return rendered markdown or error
	 */
	private String renderPageContent(String page, String language, String contextPath) {
		String resourceName = "/documentation/" + language + "/" + page + ".md";

		// read schema from resource file
		InputStream is = this.getClass().getResourceAsStream(resourceName);
		if (is == null) return "NOT AVAILABLE";

		// if no resouce exists do not run updater
		try {
			if (is.available() == 0) return "NOT AVAILABLE";
		} catch (IOException e) {
			return "IOEXCEPTION: " + e.getMessage();
		}

		StringBuilder sb = new StringBuilder();
		// read lines
		try {
			String line;
			BufferedReader in = new BufferedReader(new InputStreamReader(is));

			while((line = in.readLine()) != null) {
				sb.append(line).append("\n");
			}
			in.close();
		} catch (IOException e) {
			return "IOEXCEPTION: " + e.getMessage();
		}

		// create markdown renderer
		PegDownProcessor markdownParser = new PegDownProcessor();

		// render markdown to HTML
		return markdownParser.markdownToHtml(sb.toString(), new PageLinkRenderer(contextPath));
	}

	/**
	 * link renderer for page context
	 */
	private class PageLinkRenderer extends LinkRenderer {
		private final String contextPath;

		public PageLinkRenderer(String contextPath) {
			this.contextPath = contextPath;
		}

		@Override
		public Rendering render(ExpLinkNode node, String text) {
			// add context path to url
			String url = contextPath + "/page/" + node.url;

			LinkRenderer.Rendering rendering = new LinkRenderer.Rendering(url, text);
			rendering = rendering.withAttribute("class", "sg-data-add");
			return StringUtils.isEmpty(node.title)?rendering:rendering.withAttribute("title", FastEncoder.encode(node.title));
		}
	}
}
