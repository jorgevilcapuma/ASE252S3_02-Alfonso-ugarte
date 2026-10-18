import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { JustificacionesComponent } from './justificaciones.component';

describe('JustificacionesComponent', () => {
  let component: JustificacionesComponent;
  let fixture: ComponentFixture<JustificacionesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JustificacionesComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(JustificacionesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});