package org.segrada.service;

import com.google.inject.Inject;
import org.segrada.model.Pictogram;
import org.segrada.model.prototype.IPictogram;
import org.segrada.service.base.AbstractRepositoryService;
import org.segrada.service.base.BinaryDataHandler;
import org.segrada.service.binarydata.BinaryDataService;
import org.segrada.service.repository.PictogramRepository;
import org.segrada.service.repository.factory.RepositoryFactory;

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
 * Pictogram service
 */
public class PictogramService extends AbstractRepositoryService<IPictogram, PictogramRepository> implements BinaryDataHandler<IPictogram> {
	/**
	 * reference to binary data service
	 */
	private final BinaryDataService binaryDataService;

	/**
	 * Constructor
	 */
	@Inject
	public PictogramService(RepositoryFactory repositoryFactory, BinaryDataService binaryDataService) {
		super(repositoryFactory, PictogramRepository.class);

		this.binaryDataService = binaryDataService;
	}

	@Override
	public IPictogram createNewInstance() {
		return new Pictogram();
	}

	@Override
	public Class<IPictogram> getModelClass() {
		return IPictogram.class;
	}

	/**
	 * Find entity by title
	 * @param title login name
	 * @return entity or null
	 */
	public IPictogram findByTitle(String title) {
		return repository.findByTitle(title);
	}

	@Override
	public void saveBinaryDataToService(IPictogram entity) {
		// cast to pictogram?
		if (!(entity instanceof  Pictogram)) return; // sanity check
		Pictogram pictogram = (Pictogram) entity;

		// save and/or replace data
		String identifier = binaryDataService.saveNewReference(entity, pictogram.getFileName(), pictogram.getMimeType(),
				pictogram.getData(), entity.getFileIdentifier());

		// issue identifier, remove data
		if (identifier != null) {
			entity.setFileIdentifier(identifier);
			pictogram.setData(null);
			pictogram.setMimeType(null);
			pictogram.setFileName(null);
		}
	}

	@Override
	public void removeBinaryDataFromService(IPictogram entity) {
		binaryDataService.removeReference(entity.getFileIdentifier());
	}

	@Override
	public boolean save(IPictogram entity) {
		// new entity?
		boolean newEntity = entity.getId()==null;

		//TODO shrink image, if needed

		// map data to binary service
		saveBinaryDataToService(entity);

		// save to db
		if (super.save(entity)) {
			// update back reference
			if (newEntity) binaryDataService.updateReferenceId(entity.getFileIdentifier(), entity);
			return true;
		}

		// error while saving: delete file reference
		removeBinaryDataFromService(entity);
		return false;
	}

	@Override
	public boolean delete(IPictogram entity) {
		removeBinaryDataFromService(entity);
		return super.delete(entity);
	}
}