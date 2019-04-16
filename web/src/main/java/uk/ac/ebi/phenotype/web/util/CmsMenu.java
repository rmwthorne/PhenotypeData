/*******************************************************************************
 * Copyright 2015 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 *******************************************************************************/
package uk.ac.ebi.phenotype.web.util;


import org.apache.commons.lang3.StringUtils;
import org.mousephenotype.cda.utilities.HttpProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CmsMenu extends HttpProxy {

	private static final Logger log = LoggerFactory.getLogger(CmsMenu.class);

	private static String publicMenu = null;

	private static final String DEFAULT_MENU = "[{\"name\":\"About IMPC\",\"link\":\"https://www.mousephenotypetest.org/about-impc/\",\"children\":[{\"name\":\"Consortium members\",\"link\":\"https://www.mousephenotypetest.org/about-impc/consortium-members/\",\"children\":[]},{\"name\":\"Our people\",\"link\":\"https://www.mousephenotypetest.org/about-impc/our-people/\",\"children\":[]},{\"name\":\"Collaborations\",\"link\":\"https://www.mousephenotypetest.org/about-impc/collaborations/\",\"children\":[]},{\"name\":\"Funding\",\"link\":\"https://www.mousephenotypetest.org/about-impc/funding/\",\"children\":[]},{\"name\":\"Animal welfare\",\"link\":\"https://www.mousephenotypetest.org/about-impc/animal-welfare/\",\"children\":[]},{\"name\":\"Governance\",\"link\":\"https://www.mousephenotypetest.org/about-impc/governance/\",\"children\":[]}]},{\"name\":\"Data\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/\",\"children\":[{\"name\":\"Gene knockout technology\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/gene-knockout-technology/\",\"children\":[]},{\"name\":\"Research highlights\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/research-highlights/\",\"children\":[{\"name\":\"Embryo vignettes\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/research-highlights/embryo-vignettes/\"},{\"name\":\"Embryo development\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/research-highlights/embryo-development/\"},{\"name\":\"Sexual dimorphism\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/research-highlights/sexual-dimorphism/\"},{\"name\":\"Cardiovascular\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/research-highlights/cardiovascular/\"},{\"name\":\"Hearing\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/research-highlights/hearing/\"},{\"name\":\"Metabolism\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/research-highlights/metabolism/\"},{\"name\":\"Translating to other species\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/research-highlights/translating-to-other-species/\"}]},{\"name\":\"Accessing the Data\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/accessing-the-data/\",\"children\":[{\"name\":\"Latest data release\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/accessing-the-data/latest-data-release/\"},{\"name\":\"Access via API\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/accessing-the-data/access-via-api/\"},{\"name\":\"Access via FTP\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/accessing-the-data/access-via-ftp/\"},{\"name\":\"Batch query\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/accessing-the-data/batch-query/\"}]},{\"name\":\"Phenotyping process (IMPReSS)\",\"link\":\"https://www.mousephenotypetest.org/understanding-the-data/phenotyping-process-impress/\",\"children\":[]}]},{\"name\":\"Human Diseases\",\"link\":\"https://www.mousephenotypetest.org/human-diseases/\",\"children\":[]},{\"name\":\"News &#038; Events\",\"link\":\"https://www.mousephenotypetest.org/news-and-events/\",\"children\":[{\"name\":\"Events\",\"link\":\"https://www.mousephenotypetest.org/news-and-events/events/\",\"children\":[]},{\"name\":\"News\",\"link\":\"https://www.mousephenotypetest.org/news-and-events/news/\",\"children\":[]},{\"name\":\"Latest Publications\",\"link\":\"https://www.mousephenotypetest.org/news-and-events/latest-publications/\",\"children\":[{\"name\":\"Papers using IMPC resources\",\"link\":\"https://www.mousephenotypetest.org/news-and-events/latest-publications/papers-using-impc-resources/\"},{\"name\":\"IMPC Papers\",\"link\":\"https://www.mousephenotypetest.org/news-and-events/latest-publications/impc-papers/\"}]}]},{\"name\":\"Blog\",\"link\":\"https://www.mousephenotypetest.org/blog/\",\"children\":[]}]";


	/**
	 * Flush the cache every so often.  This is triggered by Spring on a schedule of
	 * fixedDelay = 10 * 60 * 1000 ,  initialDelay = 500
	 */
	@CacheEvict(allEntries = true, value = "menu")
	@Scheduled(fixedDelay = 1000 * 5 ,  initialDelay = 500)
	public void resetMenu() {
		System.out.println("Clearing menu cache");
		publicMenu = null;
	}

	/**
	 * Get the current menus from the CMS system -- both the main menu and the footer
	 *
	 * @return a JSON representation of the CMS menu
	 */
	@Cacheable(sync = true, value = "menu")
	public List<MenuItem> getCmsMenu(String cmsBaseUrl) {
		
		String content = publicMenu;
		Random randomGenerator = new Random();

		try {

			if (publicMenu == null || randomGenerator.nextInt(10000) == 1) {
				log.debug("Menu expired or first load, attempting to get CMS menu.");

				// Default is to add the protocol to the url if it's missing
				URL url = new URL("https:" + cmsBaseUrl + "/jsonmenu");
				if (cmsBaseUrl.contains("http")) {
					url = new URL(cmsBaseUrl + "/jsonmenu");
				}

				publicMenu = this.getContent(url).replaceAll("\\\\", "");
				content = publicMenu;
			}

			if (StringUtils.isEmpty(content)) {
				throw new Exception("Error retrieving CMS menu");
			}

		} catch (Exception e) {
			// If we can't get the menu, default to the logged out menu
			log.error("Cannot retrieve menu from CMS. Using default menu.", e);
			content = DEFAULT_MENU;
		}

		content = content.replaceAll("https://www.mousephenotypetest.org", cmsBaseUrl);

		try {
			final JSONArray jsonMenu = new JSONArray(content);
			return getMenu(jsonMenu);
		} catch (JSONException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}

	}


	/**
	 * Specific transformer for the CMS menu from JSON representation of the menu
	 * to a list of MenuItems of each top level and it's children
	 *
	 * @param menu JSONArray of menu items
	 * @return List of MenuItems representing the menu
	 * @throws JSONException when JSON is poorly formatted or otherwise incorrect
	 */
	public List<MenuItem> getMenu (JSONArray menu) throws JSONException {
		List<MenuItem> menuItems = new ArrayList<>();

		for (int i = 0; i < menu.length(); i++) {
			JSONObject topLevel = (JSONObject) menu.get(i);
			MenuItem   mi       = new MenuItem(topLevel.getString("link"), topLevel.getString("name"));

			JSONArray children = topLevel.getJSONArray("children");
			for (int j = 0; j < children.length(); j++) {
				JSONObject child = (JSONObject) children.get(j);
				MenuItem childMi = new MenuItem(child.getString("link"), child.getString("name"));

				JSONArray grandChildren = child.getJSONArray("children");
				for (int k = 0; k < grandChildren.length(); k++) {
					JSONObject grandChild = (JSONObject) grandChildren.get(k);
					MenuItem grandChildMi = new MenuItem(grandChild.getString("link"), grandChild.getString("name"));

					childMi.addChild(grandChildMi);
				}

				mi.addChild(childMi);
			}

			menuItems.add(mi);
		}

		return menuItems;
	}

	/**
	 * Menu ites include specific wordpress classes and identifiers that need to go along with the menu
	 * items in order to render correctly with the CSS.
	 */
	public class MenuItem {
		String url;
		String name;
		String cssId;
		String mobileCssId;
		String menuId;
		List<MenuItem> children;

		public MenuItem(String url, String name) {
			this.url = url;
			this.name = name;
		}

		public void addChild(MenuItem menuItem) {
			if (children == null) { children = new ArrayList<>(); }
			this.children.add(menuItem);
		}

		public String getUrl() {
			return url;
		}

		public String getName() {
			return name;
		}

		public String getCssId() {

			String cssId = "";

			if (name.toLowerCase().contains("about")) {
				cssId = "menu-item-16";
			} else if (name.toLowerCase().contains("data")) {
				cssId = "menu-item-17";
			} else if (name.toLowerCase().contains("disease")) {
				cssId = "menu-item-18";
			} else if (name.toLowerCase().contains("news")) {
				cssId = "menu-item-19";
			} else if (name.toLowerCase().contains("blog")) {
				cssId = "menu-item-983";
			}

			return cssId;
		}

		public String getMobileCssId() {

			String cssId = "";

			if (name.toLowerCase().contains("about")) {
				cssId = "object-id-7";
			} else if (name.toLowerCase().contains("data")) {
				cssId = "object-id-8";
			} else if (name.toLowerCase().contains("disease")) {
				cssId = "object-id-9";
			} else if (name.toLowerCase().contains("news")) {
				cssId = "object-id-10";
			} else if (name.toLowerCase().contains("blog")) {
				cssId = "object-id-978";
			}

			return cssId;
		}

		public String getMenuId() {

			String menuId = "";

			if (name.toLowerCase().contains("about")) {
				menuId = "about-menu";
			} else if (name.toLowerCase().contains("data")) {
				menuId = "data-menu";
			} else if (name.toLowerCase().contains("news")) {
				menuId = "news-menu";
			}

			return menuId;
		}

		public List<MenuItem> getChildren() {
			return children;
		}
	}
}