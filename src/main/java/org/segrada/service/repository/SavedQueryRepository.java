package org.segrada.service.repository;

import org.segrada.model.prototype.ISavedQuery;
import org.segrada.model.prototype.IUser;
import org.segrada.service.repository.prototype.CRUDRepository;
import org.segrada.service.repository.prototype.PaginatingRepositoryOrService;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Copyright 2016 Maximilian Kalus [segrada@auxnet.de]
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
 * Repository for Saved Query Services
 */
public interface SavedQueryRepository extends CRUDRepository<ISavedQuery>, PaginatingRepositoryOrService<ISavedQuery> {
	/**
	 * Find list of saved query by filter
	 * @param user owner of saved query (null for all)
	 * @param type to find (null for all)
	 * @param title to find (null for all)
	 * @return list of saved queries found (or empty list if none)
	 */
	List<ISavedQuery> findAllBy(@Nullable IUser user, @Nullable String type, @Nullable String title);
}
