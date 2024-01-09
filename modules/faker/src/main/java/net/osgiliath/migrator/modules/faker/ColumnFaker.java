package net.osgiliath.migrator.modules.faker;

/*-
 * #%L
 * faker
 * %%
 * Copyright (C) 2021 Osgiliath
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import net.osgiliath.migrator.core.api.metamodel.model.FieldEdge;
import net.osgiliath.migrator.core.api.metamodel.model.MetamodelVertex;
import org.jgrapht.Graph;

public class ColumnFaker extends AbstractFaker<Object> {


	public ColumnFaker(MetamodelVertex metamodel, String columnName, Graph<MetamodelVertex, FieldEdge> metaModelGraph) {
		super(metamodel, columnName, metaModelGraph);
	}

	@Override
	protected Object evaluateField(Object fieldValue) {
		return super.fake(fieldValue.toString());
	}
}
