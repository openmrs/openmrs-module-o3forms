/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.o3forms.api.impl;

import static org.openmrs.module.o3forms.O3FormsConstants.DEFAULT_FORMAT;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_ALIAS;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_ANSWERS;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_CONCEPT;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_FORM;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_FORM_NAME;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_IS_SUBFORM;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_LABEL;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_NAME;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_PAGE;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_PAGES;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_QUESTIONS;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_QUESTION_ID;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_QUESTION_OPTIONS;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_REFERENCE;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_REFERENCED_FORMS;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_SECTION;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_SECTIONS;
import static org.openmrs.module.o3forms.O3FormsConstants.SCHEMA_KEY_SUBFORM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Form;
import org.openmrs.FormResource;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.api.DatatypeService;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.ClobDatatypeStorage;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.o3forms.api.exceptions.FormResourcesNotFoundException;
import org.openmrs.module.o3forms.api.exceptions.FormSchemaNotFoundException;
import org.openmrs.module.o3forms.api.O3FormsService;
import org.openmrs.module.o3forms.api.exceptions.FormNotFoundException;
import org.openmrs.module.o3forms.api.exceptions.FormSchemaReadException;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.util.LocaleUtility;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class O3FormsServiceImpl extends BaseOpenmrsService implements O3FormsService {
	
	private static final Logger log = LoggerFactory.getLogger(O3FormsServiceImpl.class);
	
	@Transactional(readOnly = true)
	@Override
	public SimpleObject compileFormSchema(String formNameOrUuid) {
		return compileFormSchemaInternal(getForm(formNameOrUuid));
	}
	
	@Transactional(readOnly = true)
	@Override
	public SimpleObject compileFormSchema(Form form) {
		return compileFormSchemaInternal(form);
	}
	
	private SimpleObject compileFormSchemaInternal(Form form) {
		// Compilation plan:
		// 1. Process any subforms
		// 2. Collect referenced forms from main form
		// 3. Load referenced forms
		// 4. Walk the tree of pages, sections, and questions, replacing each reference element with the page, section or
		//    question it references.
		SimpleObject formSchema = getFormSchema(form);
		
		formSchema.put("uuid", form.getUuid());
		
		if (form.getEncounterType() != null) {
			Object encounterTypeObject = ConversionUtil.convertToRepresentation(form.getEncounterType(),
			    new CustomRepresentation(DEFAULT_FORMAT));
			formSchema.put("encounterType", encounterTypeObject);
		}
		
		return applyReferences(applySubForms(formSchema));
	}
	
	@Transactional(readOnly = true)
	@Override
	public SimpleObject getConceptReferences(SimpleObject compiledForm) {
		return getConceptReferences(compiledForm, new CustomRepresentation(DEFAULT_FORMAT));
	}
	
	@Transactional(readOnly = true)
	@Override
	public SimpleObject getConceptReferences(SimpleObject compiledForm, Representation conceptRepresentation) {
		if (compiledForm == null || compiledForm.isEmpty()) {
			return new SimpleObject(0);
		}
		
		final LinkedHashSet<String> conceptReferences = new LinkedHashSet<>();
		// load all concepts
		Object pagesObject = compiledForm.get(SCHEMA_KEY_PAGES);
		if (pagesObject instanceof List) {
			for (Object page : (List<?>) pagesObject) {
				if (page instanceof Map) {
					Object sectionsObject = ((Map<?, ?>) page).get(SCHEMA_KEY_SECTIONS);
					if (sectionsObject instanceof List) {
						for (Object section : (List<?>) sectionsObject) {
							if (section instanceof Map) {
								Object questionsObject = ((Map<?, ?>) section).get(SCHEMA_KEY_QUESTIONS);
								walkQuestions((List<?>) questionsObject, questionMap -> {
									{
										Object conceptObject = questionMap.get(SCHEMA_KEY_CONCEPT);
										if (conceptObject instanceof String && !((String) conceptObject).isEmpty()) {
											conceptReferences.add((String) conceptObject);
										}
									}
									
									Object questionOptionsObject = questionMap.get(SCHEMA_KEY_QUESTION_OPTIONS);
									if (questionOptionsObject instanceof Map) {
										Map<?, ?> questionOptions = (Map<?, ?>) questionOptionsObject;
										
										Object conceptObject = questionOptions.get(SCHEMA_KEY_CONCEPT);
										if (conceptObject instanceof String && !((String) conceptObject).isEmpty()) {
											conceptReferences.add((String) conceptObject);
										}
										
										Object answersObject = questionOptions.get(SCHEMA_KEY_ANSWERS);
										if (answersObject instanceof List) {
											for (Object answer : (List<?>) answersObject) {
												if (answer instanceof Map) {
													Object answerConceptObject = ((Map<?, ?>) answer)
													        .get(SCHEMA_KEY_CONCEPT);
													if (answerConceptObject instanceof String
													        && !((String) answerConceptObject).isEmpty()) {
														conceptReferences.add((String) answerConceptObject);
													}
												}
											}
										}
									}
									
									return null;
								});
							}
						}
					}
				}
			}
			
			if (conceptRepresentation == null) {
				conceptRepresentation = new CustomRepresentation(DEFAULT_FORMAT);
			}
			
			ConceptService conceptService = Context.getConceptService();
			SimpleObject result = new SimpleObject();
			for (String conceptReference : conceptReferences) {
				try {
					if (StringUtils.isBlank(conceptReference)) {
						continue;
					}
					
					Concept concept = null;
					// handle UUIDs
					if (isValidUuid(conceptReference)) {
						concept = conceptService.getConceptByUuid(conceptReference);
					}
					
					if (concept == null) {
						// handle mappings
						int idx = conceptReference.indexOf(':');
						if (idx >= 0 && idx < conceptReference.length() - 1) {
							String conceptSource = conceptReference.substring(0, idx);
							String conceptCode = conceptReference.substring(idx + 1);
							concept = conceptService.getConceptByMapping(conceptCode, conceptSource, false);
						}
					}
					
					if (concept != null) {
						result.put(conceptReference, ConversionUtil.convertToRepresentation(concept, conceptRepresentation));
					}
				}
				catch (APIException e) {
					log.warn("Error while loading concept reference {}", conceptReference, e);
				}
			}
			
			return result;
		}
		
		return new SimpleObject(0);
	}
	
	@Transactional(readOnly = true)
	@Override
	public Map<String, ?> getTranslations(String formNameOrUuid) {
		return getTranslationsInternal(getFormResources(formNameOrUuid));
	}
	
	@Transactional(readOnly = true)
	@Override
	public Map<String, ?> getTranslations(Form form) {
		return getTranslationsInternal(getFormResources(form));
	}
	
	private static boolean isValidUuid(String uuid) {
		return uuid != null
		        && (uuid.length() == 36 || uuid.length() == 38 || uuid.indexOf(' ') < 0 || uuid.indexOf('.') < 0);
	}
	
	private SimpleObject applySubForms(SimpleObject formSchema) {
		Map<String, SimpleObject> subforms = new HashMap<>();
		
		Object pagesObject = formSchema.get(SCHEMA_KEY_PAGES);
		
		if (pagesObject instanceof List) {
			walkPages((List<?>) pagesObject, (page) -> {
				Object isSubformObject = page.get(SCHEMA_KEY_IS_SUBFORM);
				
				if (isSubformObject instanceof Boolean && !((Boolean) isSubformObject)) {
					return WalkState.CONTINUE;
				} else if (isSubformObject instanceof String && !Boolean.parseBoolean((String) isSubformObject)) {
					return WalkState.CONTINUE;
				}
				
				Object subformObject = page.get(SCHEMA_KEY_SUBFORM);
				
				if (!(subformObject instanceof Map)) {
					return WalkState.CONTINUE;
				}
				
				@SuppressWarnings("unchecked")
				Map<String, Object> subform = (Map<String, Object>) subformObject;
				
				Object subformNameObject = subform.get(SCHEMA_KEY_NAME);
				
				if (!(subformNameObject instanceof String)) {
					return WalkState.CONTINUE;
				}
				
				String subformName = (String) subformNameObject;
				
				try {
					SimpleObject subformJson = subforms.computeIfAbsent(subformName, this::getFormSchema);
					applySubForms(subformJson);
					subform.put(SCHEMA_KEY_FORM, subformJson);
				}
				catch (FormNotFoundException | FormSchemaNotFoundException | FormSchemaReadException e) {
					log.error("Exception occurred while trying to read subform {}", subformName, e);
					return WalkState.CONTINUE;
				}
				
				return WalkState.CONTINUE;
			});
		}
		
		return formSchema;
	}
	
	private SimpleObject applyReferences(SimpleObject formSchema) {
		return applyReferences(formSchema, null);
	}
	
	private SimpleObject applyReferences(SimpleObject formSchema, Map<String, Map<String, Object>> referencedForms) {
		if (referencedForms == null) {
			referencedForms = getReferencedForms(formSchema).orElse(null);
		} else {
			getReferencedForms(formSchema).ifPresent(referencedForms::putAll);
		}
		
		final Map<String, Map<String, Object>> finalReferencedForms = referencedForms;
		
		if (finalReferencedForms != null) {
			Object pagesObject = formSchema.get(SCHEMA_KEY_PAGES);
			
			if (pagesObject instanceof List) {
				walkPages((List<?>) pagesObject, (pageMap) -> {
					Object isSubformObject = pageMap.get(SCHEMA_KEY_IS_SUBFORM);
					if ((isSubformObject instanceof Boolean && (Boolean) isSubformObject)
					        || (isSubformObject instanceof String && Boolean.parseBoolean((String) isSubformObject))) {
						applyReferences(formSchema, finalReferencedForms);
					}
					
					Set<String> pageExcludedQuestions = new HashSet<>();
					if (pageMap.containsKey(SCHEMA_KEY_REFERENCE)) {
						Map<?, ?> referenceMap = getReferenceObjectFromItem(pageMap).orElse(null);
						pageMap.clear();
						
						if (referenceMap == null) {
							return WalkState.CONTINUE;
						}
						
						if (!referenceMap.containsKey(SCHEMA_KEY_PAGE)) {
							log.error("Form compilation - reference missing page attribute: {}", referenceMap);
							return WalkState.CONTINUE;
						}
						
						Object referencePageObject = referenceMap.get(SCHEMA_KEY_PAGE);
						if (!(referencePageObject instanceof String)) {
							log.error("Form compilation - reference page is not a JSON string: {}", referencePageObject);
							return WalkState.CONTINUE;
						}
						
						Map<String, Object> referencedForm = getReferencedFormForItem(finalReferencedForms, referenceMap)
						        .orElse(null);
						
						if (referencedForm == null) {
							return WalkState.CONTINUE;
						}
						
						pageExcludedQuestions.addAll(getExclusions(referenceMap));
						
						getPageByLabel(referencedForm, (String) referencePageObject).map(p -> {
							pageMap.putAll(p);
							return p;
						});
					}
					
					if (pageMap.containsKey(SCHEMA_KEY_SECTIONS)) {
						Object sectionsObject = pageMap.get(SCHEMA_KEY_SECTIONS);
						
						if (!(sectionsObject instanceof List)) {
							log.warn("Form compilation - section array contains a non-object entry: {}", sectionsObject);
							return WalkState.CONTINUE;
						}
						
						walkSections((List<?>) sectionsObject, (sectionMap) -> {
							Set<String> sectionExcludedQuestions = new HashSet<>(pageExcludedQuestions);
							
							// wrapped in a one-time do... while loop so that continue statements only break out of the
							// while loop, so that we process all questions for all sections
							do {
								if (sectionMap.containsKey(SCHEMA_KEY_REFERENCE)) {
									Map<?, ?> referenceMap = getReferenceObjectFromItem(sectionMap).orElse(null);
									sectionMap.clear();
									
									if (referenceMap == null) {
										continue;
									}
									
									Object referencePageObject = referenceMap.get(SCHEMA_KEY_PAGE);
									if (!(referencePageObject instanceof String)) {
										log.error("Form compilation - reference page is not a JSON string: {}",
										    referencePageObject);
										continue;
									}
									
									Object referenceSectionObject = referenceMap.get(SCHEMA_KEY_SECTION);
									if (!(referenceSectionObject instanceof String)) {
										log.error("Form compilation - reference section is not a JSON string: {}",
										    referencePageObject);
										continue;
									}
									
									Map<String, Object> referencedForm = getReferencedFormForItem(finalReferencedForms,
									    referenceMap).orElse(null);
									
									if (referencedForm == null) {
										continue;
									}
									
									sectionExcludedQuestions.addAll(getExclusions(referenceMap));
									
									getSectionByPageAndLabel(referencedForm, (String) referencePageObject,
									    (String) referenceSectionObject).map(s -> {
										    sectionMap.putAll(s);
										    return s;
									    });
								}
							} while (false);
							
							Object questionsObj = sectionMap.get(SCHEMA_KEY_QUESTIONS);
							if (!(questionsObj instanceof List)) {
								log.warn("Form compilation - question array contains a non-object entry: {}", questionsObj);
								return WalkState.CONTINUE;
							}
							
							walkQuestions((List<?>) questionsObj, questionMap -> {
								if (questionMap.containsKey(SCHEMA_KEY_REFERENCE)) {
									Map<?, ?> referenceMap = getReferenceObjectFromItem(questionMap).orElse(null);
									questionMap.clear();
									
									if (referenceMap == null) {
										return WalkState.CONTINUE;
									}
									
									Object referenceQuestionIdObject = referenceMap.get(SCHEMA_KEY_QUESTION_ID);
									if (!(referenceQuestionIdObject instanceof String)) {
										return WalkState.CONTINUE;
									}
									
									Map<String, Object> referencedForm = getReferencedFormForItem(finalReferencedForms,
									    referenceMap).orElse(null);
									
									if (referencedForm == null) {
										return WalkState.CONTINUE;
									}
									
									getQuestionById(referencedForm, (String) referenceQuestionIdObject).map(q -> {
										questionMap.putAll(q);
										return q;
									});
								}
								
								return WalkState.CONTINUE;
							});
							
							if (!sectionExcludedQuestions.isEmpty()) {
								walkQuestions((List<?>) questionsObj, (ignored, questionList) -> {
									questionList.removeIf(question -> {
										Map<?, ?> questionMap = (Map<?, ?>) question;
										
										if (!questionMap.containsKey("id")) {
											return false;
										}
										
										Object questionIdObject = questionMap.get("id");
										
										if (!(questionIdObject instanceof String)) {
											return false;
										}
										
										return sectionExcludedQuestions.contains(questionIdObject);
									});
									
									return WalkState.NEXT_LIST;
								});
							}
							
							return WalkState.CONTINUE;
						});
					}
					
					return WalkState.CONTINUE;
				});
			}
		}
		
		return formSchema;
	}
	
	private Map<String, ?> getTranslationsInternal(List<FormResource> formResources) {
		return Optional.ofNullable(formResources).map(frs -> {
			SimpleObject formSchema = getFormSchemaFromResources(formResources);
			List<?> referencedForms = getReferencedFormList(formSchema).orElse(Collections.emptyList());
			
			// potentially breaking in the future because of down-cast
			LinkedHashSet<Locale> locales = (LinkedHashSet<Locale>) LocaleUtility.getLocalesInOrder();
			
			Map<String, Object> result = referencedForms.stream()
			        .map(referencedForm -> getTranslationsByPreferredLocales(locales, referencedForm))
			        .filter(Optional::isPresent).map(Optional::get).reduce(new LinkedHashMap<String, Object>(), (acc, i) -> {
				        acc.putAll(i);
				        return acc;
			        });
			        
			getTranslationsByPreferredLocales(locales, formResources).ifPresent(result::putAll);
			
			return result;
		}).orElse(Collections.emptyMap());
	}
	
	private Optional<Map<String, Object>> getTranslationsByPreferredLocales(LinkedHashSet<Locale> locales,
	        Object referencedForm) {
		if (!(referencedForm instanceof Map)) {
			return Optional.empty();
		}
		
		Map<?, ?> referencedFormMap = (Map<?, ?>) referencedForm;
		
		if (!referencedFormMap.containsKey(SCHEMA_KEY_FORM_NAME)) {
			return Optional.empty();
		}
		
		Object referencedFormName = referencedFormMap.get(SCHEMA_KEY_FORM_NAME);
		
		if (!(referencedFormName instanceof String)) {
			return Optional.empty();
		}
		
		try {
			return getTranslationsByPreferredLocales(locales, getFormResources((String) referencedFormName));
		}
		catch (APIException e) {
			log.warn("Exception caught while trying to load translations for form {}", referencedFormName, e);
			return Optional.empty();
		}
	}
	
	private Optional<Map<String, Object>> getTranslationsByPreferredLocales(LinkedHashSet<Locale> locales,
	        List<FormResource> formResources) {
		for (Locale locale : locales) {
			String needle = "_translations_" + locale.toLanguageTag();
			for (FormResource resource : formResources) {
				if (resource.getName() != null && resource.getName().endsWith(needle)) {
					try {
						SimpleObject object = loadJsonClob(resource.getValueReference());
						if (object != null) {
							Object translationsObject = object.get("translations");
							if (translationsObject instanceof Map) {
								return Optional.ofNullable((Map<String, Object>) translationsObject);
							}
							
							return Optional.empty();
						}
					}
					catch (APIException e) {
						if (resource.getForm() != null && resource.getForm().getName() != null) {
							log.warn("Exception caught while trying to load translations for form {} in locale {}",
							    resource.getForm().getName(), locale.toLanguageTag(), e);
						} else {
							log.warn("Exception caught while trying to load translations in locale {} from resource {}",
							    locale.toLanguageTag(), resource.getUuid(), e);
						}
					}
				}
			}
		}
		
		return Optional.empty();
	}
	
	/**
	 * This is an <b>internal</b> method made "protected" for testing
	 *
	 * @param formNameOrUuid The form name or uuid to find the form object for
	 * @return The form object
	 * @throws FormNotFoundException if the form cannot be found
	 */
	Form getForm(String formNameOrUuid) {
		if (formNameOrUuid == null || formNameOrUuid.isEmpty()) {
			throw new FormNotFoundException("getForm() must be called with a form name");
		}
		
		FormService formService = Context.getFormService();
		
		Form form = formService.getFormByUuid(formNameOrUuid);
		if (form == null) {
			// TODO Fix the implementation of getForm() in Core.
			List<Form> forms = formService.getForms(formNameOrUuid, null, null, null, null, null, null);
			if (!forms.isEmpty()) {
				if (forms.size() == 1) {
					form = forms.get(0);
				} else {
					// logic here:
					// 1. Favor a published, unretired form above all else
					// 2. Favor an unretired form if a published one cannot be found
					// 3. Favor the newest published but retired form
					// 4. Favor the newest retired form
					forms.sort((form1, form2) -> {
						Date form1Date = form1.getDateCreated();
						if (form1.getDateChanged() != null) {
							form1Date = form1.getDateChanged();
						}
						
						Date form2Date = form2.getDateCreated();
						if (form2.getDateChanged() != null) {
							form2Date = form2.getDateChanged();
						}
						
						return -1 * form1Date.compareTo(form2Date);
					});
					
					for (Form candidateForm : forms) {
						if (form == null) {
							form = candidateForm;
							if (!form.getRetired() && form.getPublished()) {
								break;
							}
						} else {
							if (!form.getRetired()) {
								if (!candidateForm.getRetired()) {
									if (candidateForm.getPublished()) {
										form = candidateForm;
										break;
									}
								}
							} else {
								if (!candidateForm.getRetired()) {
									form = candidateForm;
								} else {
									if (!form.getPublished() && candidateForm.getPublished()) {
										form = candidateForm;
									}
								}
							}
						}
					}
				}
			}
		}
		
		if (form == null) {
			throw new FormNotFoundException();
		}
		
		return form;
	}
	
	private List<FormResource> getFormResources(String formNameOrUuid) {
		return getFormResources(getForm(formNameOrUuid));
	}
	
	private List<FormResource> getFormResources(Form form) {
		FormService formService = Context.getFormService();
		
		if (form == null) {
			throw new FormSchemaNotFoundException();
		}
		
		List<FormResource> formResources = (List<FormResource>) formService.getFormResourcesForForm(form);
		
		if (formResources == null || formResources.isEmpty()) {
			throw new FormResourcesNotFoundException();
		}
		
		return formResources;
	}
	
	private SimpleObject getFormSchema(String formNameOrUuid) {
		return getFormSchemaFromResources(getFormResources(getForm(formNameOrUuid)));
	}
	
	private SimpleObject getFormSchema(Form form) {
		return getFormSchemaFromResources(getFormResources(form));
	}
	
	private SimpleObject getFormSchemaFromResources(List<FormResource> formResources) {
		return formResources.stream().filter(fr -> "JSON schema".equals(fr.getName())).findFirst()
		        .map(FormResource::getValueReference).map(O3FormsServiceImpl::loadJsonClob)
		        .orElseThrow(FormSchemaNotFoundException::new);
	}
	
	private Optional<List<?>> getReferencedFormList(SimpleObject formSchema) {
		if (!formSchema.containsKey(SCHEMA_KEY_REFERENCED_FORMS)) {
			return Optional.empty();
		}
		
		Object referencedFormsObject = formSchema.get(SCHEMA_KEY_REFERENCED_FORMS);
		if (!(referencedFormsObject instanceof List) || ((List<?>) referencedFormsObject).isEmpty()) {
			return Optional.empty();
		}
		
		return Optional.of((List<?>) referencedFormsObject);
	}
	
	private Optional<Map<String, Map<String, Object>>> getReferencedForms(SimpleObject formSchema) {
		return getReferencedFormList(formSchema).map(referencedForms -> {
			Map<String, Map<String, Object>> result = new LinkedHashMap<>(referencedForms.size());
			for (Object referencedForm : referencedForms) {
				if (!(referencedForm instanceof Map)) {
					log.warn("Form compilation - Referenced form is not a JSON object: {}", referencedForm);
					continue;
				}
				
				Map<?, ?> referencedFormMap = (Map<?, ?>) referencedForm;
				
				if (!referencedFormMap.containsKey(SCHEMA_KEY_FORM_NAME)) {
					log.warn("Form compilation - Referenced form does not have the attribute formName: {}",
					    referencedFormMap);
					continue;
				}
				
				Object referencedFormName = referencedFormMap.get(SCHEMA_KEY_FORM_NAME);
				
				if (!(referencedFormName instanceof String)) {
					log.warn("Form compilation - Referenced form formName is not a JSON string: {}", referencedFormMap);
					continue;
				}
				
				Object aliasObject = referencedFormMap.get(SCHEMA_KEY_ALIAS);
				
				if (!(aliasObject instanceof String)) {
					aliasObject = referencedFormName;
				}
				
				if (result.containsKey((String) aliasObject)) {
					log.debug("Form compilation - Skipping already loaded form");
					continue;
				}
				
				try {
					result.put((String) aliasObject, getFormSchema((String) referencedFormName));
				}
				catch (FormNotFoundException | FormResourcesNotFoundException | FormSchemaNotFoundException
				        | FormSchemaReadException e) {
					log.warn("Form compilation - Could not load schema for form {}", referencedFormName, e);
				}
			}
			
			return Optional.of(result);
		}).orElse(Optional.empty());
	}
	
	private static Optional<Map<?, ?>> getReferenceObjectFromItem(Map<?, ?> item) {
		Object reference = item.get(SCHEMA_KEY_REFERENCE);
		
		if (!(reference instanceof Map)) {
			log.error("Form compilation - reference not a JSON object: {}", reference);
			return Optional.empty();
		}
		
		return Optional.of((Map<?, ?>) reference);
	}
	
	private static Optional<Map<String, Object>> getReferencedFormForItem(Map<String, Map<String, Object>> referencedForms,
	        Map<?, ?> referenceMap) {
		if (!referenceMap.containsKey(SCHEMA_KEY_FORM)) {
			log.error("Form compilation - reference missing form attribute: {}", referenceMap);
			return Optional.empty();
		}
		
		Object referencedFormObj = referenceMap.get(SCHEMA_KEY_FORM);
		
		if (!(referencedFormObj instanceof String)) {
			log.error("Form compilation - referenced form is not a string: {}", referenceMap);
			return Optional.empty();
		}
		
		String referencedFormAlias = (String) referencedFormObj;
		
		if (!referencedForms.containsKey(referencedFormAlias)) {
			log.error("Form compilation - referenced form alias not found: {}", referenceMap);
			return Optional.empty();
		}
		
		return Optional.ofNullable(
		    SerializationUtils.clone((LinkedHashMap<String, Object>) referencedForms.get(referencedFormAlias)));
	}
	
	private static Optional<Map<String, Object>> getPageByLabel(Map<String, Object> formSchema, String pageLabel) {
		if (!formSchema.containsKey(SCHEMA_KEY_PAGES)) {
			log.error("Form compilation - referenced form {} does not define any pages", formSchema.get("name"));
			return Optional.empty();
		}
		
		Object pagesObj = formSchema.get(SCHEMA_KEY_PAGES);
		if (!(pagesObj instanceof List)) {
			log.error("Form compilation - referenced form {} does not define pages as a JSON array: {}",
			    formSchema.get("name"), pagesObj);
			return Optional.empty();
		}
		
		return getJsonObjectByLabel((List<?>) pagesObj, pageLabel);
	}
	
	private static Optional<Map<String, Object>> getSectionByPageAndLabel(Map<String, Object> formSchema, String pageLabel,
	        String sectionLabel) {
		return getPageByLabel(formSchema, pageLabel).map(page -> {
			if (!page.containsKey(SCHEMA_KEY_SECTIONS)) {
				log.error("Form compilation - referenced page {} in form {} does not define any sections", pageLabel,
				    formSchema.get("name"));
				return null;
			}
			
			Object sectionsObj = page.get(SCHEMA_KEY_SECTIONS);
			if (!(sectionsObj instanceof List)) {
				log.error("Form compilation - referenced page {} in form {} does not define pages as a JSON array: {}",
				    pageLabel, formSchema.get("name"), sectionsObj);
				return null;
			}
			
			return getJsonObjectByLabel((List<?>) sectionsObj, sectionLabel).orElse(null);
		});
	}
	
	/**
	 * States for the various {@link #walkQuestions(List, Function)} implementations
	 */
	private enum WalkState {
		/**
		 * Default state; process the next item in the list
		 */
		CONTINUE,
		/**
		 * Used to indicate no further processing is needed, so the loop is exited
		 */
		BREAK,
		/**
		 * Used to indicate that no further processing of individual questions are needed, but if a question
		 * contains other questions, those questions will be processed.
		 */
		NEXT_LIST;
	}
	
	private static void walkPages(List<?> pages, Function<Map<String, Object>, WalkState> handler) {
		walkPages(pages, (pageMap, ignored) -> handler.apply(pageMap));
	}
	
	private static void walkPages(List<?> pages, BiFunction<Map<String, Object>, List<?>, WalkState> handler) {
		WalkState currentState = WalkState.CONTINUE;
		
		// wrap list in a new list to avoid ConcurrentModificationExceptions
		for (Object page : pages) {
			if (!(page instanceof Map)) {
				log.info("Form compilation - pages array contains a non-object entry: {}", page);
				continue;
			}
			
			@SuppressWarnings("unchecked")
			Map<String, Object> pageMap = (Map<String, Object>) page;
			
			if (currentState == WalkState.CONTINUE) {
				WalkState nextState = handler.apply(pageMap, pages);
				if (nextState == WalkState.BREAK) {
					return;
				} else if (nextState != null) {
					currentState = nextState;
				}
			}
		}
	}
	
	private static void walkSections(List<?> sections, Function<Map<String, Object>, WalkState> handler) {
		walkSections(sections, (sectionMap, ignored) -> handler.apply(sectionMap));
	}
	
	private static void walkSections(List<?> sections, BiFunction<Map<String, Object>, List<?>, WalkState> handler) {
		WalkState currentState = WalkState.CONTINUE;
		
		// wrap list in a new list to avoid ConcurrentModificationExceptions
		for (Object section : new ArrayList<>(sections)) {
			if (!(section instanceof Map)) {
				log.info("Form compilation - sections array contains a non-object entry: {}", section);
				continue;
			}
			
			@SuppressWarnings("unchecked")
			Map<String, Object> sectionMap = (Map<String, Object>) section;
			
			if (currentState == WalkState.CONTINUE) {
				WalkState nextState = handler.apply(sectionMap, sections);
				if (nextState == WalkState.BREAK) {
					return;
				} else if (nextState != null) {
					currentState = nextState;
				}
			}
		}
	}
	
	/**
	 * Helper to recursively traverse a list of questions, since questions can contain other questions.
	 * By default, the supplied handler will be called with each question as it is encountered. Use the
	 * {@link WalkState} values to control how questions are handed to the handler.
	 *
	 * @param questions A list of objects that are potentially O3 form question objects
	 * @param handler A callback that is called with a map representing the question
	 */
	private static void walkQuestions(List<?> questions, Function<Map<String, Object>, WalkState> handler) {
		walkQuestions(questions, (questionMap, ignored) -> handler.apply(questionMap));
	}
	
	/**
	 * Helper to recursively traverse a list of questions, since questions can contain other questions.
	 * By default, the supplied handler will be called with each question as it is encountered. Use the
	 * {@link WalkState} values to control how questions are handed to the handler.
	 *
	 * @param questions A list of objects that are potentially O3 form question objects
	 * @param handler A callback that is called with a map representing the question and the list the
	 *            question appeared in
	 */
	private static void walkQuestions(List<?> questions, BiFunction<Map<String, Object>, List<?>, WalkState> handler) {
		WalkState currentState = WalkState.CONTINUE;
		
		// wrap list in a new list to avoid ConcurrentModificationExceptions
		for (Object question : new ArrayList<>(questions)) {
			if (!(question instanceof Map)) {
				log.info("Form compilation - questions array contains a non-object entry: {}", question);
				continue;
			}
			
			@SuppressWarnings("unchecked")
			Map<String, Object> questionMap = (Map<String, Object>) question;
			
			if (currentState == WalkState.CONTINUE) {
				WalkState nextState = handler.apply(questionMap, questions);
				if (nextState == WalkState.BREAK) {
					return;
				} else if (nextState != null) {
					currentState = nextState;
				}
			}
			
			if (questionMap.containsKey(SCHEMA_KEY_QUESTIONS)) {
				Object questionsObj = questionMap.get(SCHEMA_KEY_QUESTIONS);
				if (!(questionsObj instanceof List)) {
					continue;
				}
				
				walkQuestions((List<?>) questionsObj, handler);
			}
		}
	}
	
	private static Optional<Map<String, Object>> getQuestionById(Map<String, Object> formSchema, String questionId) {
		if (!formSchema.containsKey(SCHEMA_KEY_PAGES)) {
			log.error("Form compilation - referenced form {} does not define any pages", formSchema.get("name"));
			return Optional.empty();
		}
		
		Object pagesObject = formSchema.get(SCHEMA_KEY_PAGES);
		if (!(pagesObject instanceof List)) {
			return Optional.empty();
		}
		
		for (Object page : (List<?>) pagesObject) {
			if (!(page instanceof Map)) {
				continue;
			}
			
			Map<?, ?> pageMap = (Map<?, ?>) page;
			if (pageMap.containsKey(SCHEMA_KEY_SECTIONS)) {
				Object sectionsObject = pageMap.get(SCHEMA_KEY_SECTIONS);
				
				if (!(sectionsObject instanceof List)) {
					continue;
				}
				
				for (Object section : (List<?>) sectionsObject) {
					if (!(section instanceof Map)) {
						continue;
					}
					
					Map<?, ?> sectionMap = (Map<?, ?>) section;
					
					Object questionsObject = sectionMap.get(SCHEMA_KEY_QUESTIONS);
					if (!(questionsObject instanceof List)) {
						continue;
					}
					
					final Map<String, Object> result = new LinkedHashMap<>();
					walkQuestions((List<?>) questionsObject, questionMap -> {
						Object questionIdObject = questionMap.get("id");
						
						if (!(questionIdObject instanceof String)) {
							return WalkState.CONTINUE;
						}
						
						if (OpenmrsUtil.nullSafeEquals(questionId, (String) questionIdObject)) {
							result.putAll(questionMap);
							return WalkState.BREAK;
						}
						
						return null;
					});
					
					// result is never null
					if (!result.isEmpty()) {
						return Optional.of(result);
					} else {
						return Optional.empty();
					}
				}
			}
		}
		
		return Optional.empty();
	}
	
	private static Set<String> getExclusions(Map<?, ?> referenceMap) {
		Object referenceExcludedQuestionsObject = referenceMap.get("excludeQuestions");
		if (referenceExcludedQuestionsObject instanceof List) {
			return ((List<?>) referenceExcludedQuestionsObject).stream().filter(q -> q instanceof String)
			        .map(q -> (String) q).collect(Collectors.toSet());
		} else {
			return Collections.emptySet();
		}
	}
	
	private static Optional<Map<String, Object>> getJsonObjectByLabel(List<?> labelledItems, String label) {
		return labelledItems.stream().filter(item -> {
			if (!(item instanceof Map)) {
				return false;
			}
			
			Map<?, ?> sectionMap = (Map<?, ?>) item;
			if (!(sectionMap.containsKey(SCHEMA_KEY_LABEL))) {
				return false;
			}
			
			Object labelObject = sectionMap.get(SCHEMA_KEY_LABEL);
			if (!(labelObject instanceof String)) {
				return false;
			}
			
			return OpenmrsUtil.nullSafeEqualsIgnoreCase(label, (String) labelObject);
		}).map(item -> (Map<String, Object>) item).findFirst();
	}
	
	private static SimpleObject loadJsonClob(String clobUuid) {
		return loadJsonClob(clobUuid, Context.getDatatypeService());
	}
	
	private static SimpleObject loadJsonClob(String clobUuid, DatatypeService datatypeService) {
		ClobDatatypeStorage jsonClob = datatypeService.getClobDatatypeStorageByUuid(clobUuid);
		
		if (jsonClob == null) {
			throw new FormSchemaNotFoundException();
		}
		
		String json = jsonClob.getValue();
		try {
			return SimpleObject.parseJson(json);
		}
		catch (IOException e) {
			throw new FormSchemaReadException(e);
		}
	}
}
